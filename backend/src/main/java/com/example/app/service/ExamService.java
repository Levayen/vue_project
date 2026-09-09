package com.example.app.service;

import com.example.app.dto.ExamDTO;
import com.example.app.dto.MonitoringDTO;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Course;
import com.example.app.entity.Enrollment;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.PaperStatus;
import com.example.app.entity.ScoreRule;
import com.example.app.entity.Student;
import com.example.app.exception.BusinessException;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRepository;
import com.example.app.repository.ExamPaperRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 考试服务（SPEC-exam-session + M6 监考）
 * 发布考试、考试列表、监考视图、强制收卷。
 */
@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamPaperRepository paperRepository;
    private final ExamAttemptRepository attemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradingPort gradingPort;
    private final ObjectMapper objectMapper;

    public ExamService(ExamRepository examRepository,
                       ExamPaperRepository paperRepository,
                       ExamAttemptRepository attemptRepository,
                       EnrollmentRepository enrollmentRepository,
                       GradingPort gradingPort,
                       ObjectMapper objectMapper) {
        this.examRepository = examRepository;
        this.paperRepository = paperRepository;
        this.attemptRepository = attemptRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.gradingPort = gradingPort;
        this.objectMapper = objectMapper;
    }

    /**
     * 发布考试
     */
    @Transactional
    public ExamDTO publish(ExamDTO dto) {
        if (dto.title() == null || dto.title().isBlank()) {
            throw BusinessException.badRequest("考试名称不能为空");
        }
        if (dto.paperId() == null) {
            throw BusinessException.badRequest("请选择试卷");
        }
        ExamPaper paper = paperRepository.findById(dto.paperId())
                .orElseThrow(() -> BusinessException.badRequest("试卷不存在"));
        if (paper.getStatus() != PaperStatus.PUBLISHED) {
            throw BusinessException.badRequest("只能基于已发布的试卷发布考试");
        }
        if (dto.startTime() == null || dto.endTime() == null) {
            throw BusinessException.badRequest("请设置考试起止时间");
        }
        if (!dto.endTime().isAfter(dto.startTime())) {
            throw BusinessException.badRequest("结束时间必须晚于开始时间");
        }
        Integer duration = dto.durationMinutes() == null ? 0 : dto.durationMinutes();
        if (duration <= 0) {
            throw BusinessException.badRequest("考试时长必须大于 0 分钟");
        }
        Integer maxAttempts = dto.maxAttempts() == null ? 1 : dto.maxAttempts();
        if (maxAttempts <= 0) {
            throw BusinessException.badRequest("考试次数必须大于 0");
        }

        Exam exam = new Exam();
        exam.setTitle(dto.title().trim());
        exam.setPaper(paper);
        exam.setCourse(paper.getCourse());
        exam.setStartTime(dto.startTime());
        exam.setEndTime(dto.endTime());
        exam.setDurationMinutes(duration);
        exam.setMaxAttempts(maxAttempts);
        exam.setScoreRule(parseScoreRule(dto.scoreRule()));
        exam.setShuffle(dto.shuffle() == null || dto.shuffle());
        exam.setOpenBook(dto.openBook() != null && dto.openBook());
        examRepository.save(exam);
        return toDTO(exam);
    }

    /**
     * 考试列表（教师视角，可按课程筛选）
     */
    public List<ExamDTO> list(Long courseId) {
        List<Exam> exams = courseId == null
                ? examRepository.findAll().stream()
                        .sorted(Comparator.comparing(Exam::getStartTime).reversed()).toList()
                : examRepository.findByCourse_IdOrderByStartTimeDesc(courseId);
        return exams.stream().map(this::toDTO).toList();
    }

    /**
     * 监考视图（M6 增强）：汇总统计 + 每位学生的状态、成绩、剩余时间、违规明细
     */
    public MonitoringDTO monitoring(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> BusinessException.notFound("考试不存在"));

        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(exam.getCourse().getId());
        List<ExamAttempt> allAttempts = attemptRepository.findByExam_IdOrderByStartTimeDesc(examId);
        LocalDateTime now = LocalDateTime.now();

        int inProgress = 0, submitted = 0, pendingReview = 0, notStarted = 0, totalViolations = 0;
        List<MonitoringDTO.StudentAttemptInfo> infos = new ArrayList<>();

        for (Enrollment en : enrollments) {
            Student student = en.getStudent();
            List<ExamAttempt> studentAttempts = allAttempts.stream()
                    .filter(a -> a.getStudentId().equals(student.getId()))
                    .toList();

            String status = "未开始";
            int attemptCount = studentAttempts.size();
            int violations = 0;
            BigDecimal score = null;
            LocalDateTime startTime = null;
            Long remainingSeconds = null;
            List<MonitoringDTO.ViolationEvent> violationEvents = new ArrayList<>();

            if (!studentAttempts.isEmpty()) {
                ExamAttempt latest = studentAttempts.get(0);
                status = latest.getStatus().name();
                violations = studentAttempts.stream()
                        .mapToInt(a -> a.getViolationCount() == null ? 0 : a.getViolationCount())
                        .sum();
                score = latest.getScore();
                startTime = latest.getStartTime();

                violationEvents = parseViolations(studentAttempts);

                if (latest.getStatus() == AttemptStatus.IN_PROGRESS) {
                    remainingSeconds = Math.max(0,
                            Duration.between(now, latest.getDeadline()).getSeconds());
                }
            }

            if (status.equals("未开始")) {
                notStarted++;
            } else if (status.equals("IN_PROGRESS")) {
                inProgress++;
            } else if (status.equals("PENDING_REVIEW")) {
                pendingReview++;
            } else if (status.equals("SUBMITTED")) {
                submitted++;
            }
            totalViolations += violations;

            infos.add(new MonitoringDTO.StudentAttemptInfo(
                    student.getId(),
                    student.getName(),
                    student.getStudentNumber(),
                    status,
                    attemptCount,
                    score,
                    violations,
                    startTime,
                    remainingSeconds,
                    violationEvents
            ));
        }

        return new MonitoringDTO(
                exam.getId(),
                exam.getTitle(),
                enrollments.size(),
                inProgress,
                submitted,
                pendingReview,
                notStarted,
                totalViolations,
                infos
        );
    }

    /**
     * 强制收卷（M6）：对指定学生或全体进行中尝试强制交卷判分
     *
     * @param examId    考试 ID
     * @param studentId 学生 ID，为 null 时强制收卷全体进行中考生
     * @return 被强制收卷的学生人数
     */
    @Transactional
    public int forceSubmit(Long examId, Long studentId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> BusinessException.notFound("考试不存在"));

        List<ExamAttempt> inProgress;
        if (studentId != null) {
            inProgress = attemptRepository
                    .findByExam_IdAndStudentIdAndStatus(examId, studentId, AttemptStatus.IN_PROGRESS)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            inProgress = attemptRepository.findByExam_IdOrderByStartTimeDesc(examId).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                    .toList();
        }

        for (ExamAttempt attempt : inProgress) {
            attempt.setSubmitTime(attempt.getDeadline());
            attemptRepository.save(attempt);
            // grade 内部置状态（含主观题时 PENDING_REVIEW）与分数、成绩记录
            gradingPort.grade(attempt);
        }
        return inProgress.size();
    }

    /**
     * 解析违规事件明细 JSON
     */
    private List<MonitoringDTO.ViolationEvent> parseViolations(List<ExamAttempt> attempts) {
        List<MonitoringDTO.ViolationEvent> events = new ArrayList<>();
        for (ExamAttempt a : attempts) {
            if (a.getViolationsJson() == null || a.getViolationsJson().isBlank()) {
                continue;
            }
            try {
                List<Map<String, String>> raw = objectMapper.readValue(
                        a.getViolationsJson(), new TypeReference<List<Map<String, String>>>() {});
                for (Map<String, String> m : raw) {
                    events.add(new MonitoringDTO.ViolationEvent(
                            m.getOrDefault("type", "UNKNOWN"),
                            m.getOrDefault("time", "")
                    ));
                }
            } catch (Exception ignored) {
                // 解析失败跳过，不影响监考视图
            }
        }
        return events;
    }

    private ScoreRule parseScoreRule(String rule) {
        if (rule == null || rule.isBlank()) {
            return ScoreRule.LAST;
        }
        try {
            return ScoreRule.valueOf(rule.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("非法成绩规则：" + rule);
        }
    }

    private ExamDTO toDTO(Exam exam) {
        Course course = exam.getCourse();
        return new ExamDTO(
                exam.getId(),
                exam.getPaper().getId(),
                exam.getPaper().getName(),
                course.getId(),
                course.getCourseName(),
                exam.getTitle(),
                exam.getStartTime(),
                exam.getEndTime(),
                exam.getDurationMinutes(),
                exam.getMaxAttempts(),
                exam.getScoreRule().name(),
                exam.getShuffle(),
                exam.getOpenBook(),
                exam.getPaper().getTotalScore()
        );
    }
}
