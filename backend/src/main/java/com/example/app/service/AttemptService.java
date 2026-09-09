package com.example.app.service;

import com.example.app.dto.ExamQuestionDTO;
import com.example.app.dto.QuestionOption;
import com.example.app.dto.StartExamResponse;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Enrollment;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.SysUser;
import com.example.app.exception.BusinessException;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRepository;
import com.example.app.repository.PaperQuestionRepository;
import com.example.app.repository.SysUserRepository;
import com.example.app.security.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生考试尝试服务（SPEC-exam-session）
 * 入场校验（选课/时间窗口/次数）、续考、答案暂存、违规上报、交卷判分。
 * 时间与身份一律以服务端为准；闭卷考试发卷脱敏（open_book=0 不返回 answer/analysis）。
 */
@Service
public class AttemptService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SysUserRepository sysUserRepository;
    private final GradingPort gradingPort;
    private final ObjectMapper objectMapper;

    /** 可注入时钟，便于单测构造时间边界；默认系统时钟 */
    private Clock clock = Clock.systemDefaultZone();

    public AttemptService(ExamRepository examRepository,
                          ExamAttemptRepository attemptRepository,
                          PaperQuestionRepository paperQuestionRepository,
                          EnrollmentRepository enrollmentRepository,
                          SysUserRepository sysUserRepository,
                          GradingPort gradingPort,
                          ObjectMapper objectMapper) {
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sysUserRepository = sysUserRepository;
        this.gradingPort = gradingPort;
        this.objectMapper = objectMapper;
    }

    /** 仅单测使用：注入固定时钟 */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 学生考试列表：返回已选课课程下的所有考试，附状态与已考成绩
     */
    public List<Map<String, Object>> myExams() {
        Long studentId = currentStudentId();
        List<Long> courseIds = enrollmentRepository.findByStudentId(studentId).stream()
                .map(en -> en.getCourse().getId())
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<Exam> exams = examRepository.findByCourse_IdInOrderByStartTimeDesc(courseIds);
        LocalDateTime now = now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Exam exam : exams) {
            List<ExamAttempt> attempts = attemptRepository
                    .findByExam_IdAndStudentIdOrderByStartTimeDesc(exam.getId(), studentId);
            // 交卷即占用次数：SUBMITTED 与 PENDING_REVIEW（待阅卷）均计入
            List<ExamAttempt> finished = attempts.stream()
                    .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                    .toList();
            long submitted = finished.size();
            boolean grading = finished.stream()
                    .anyMatch(a -> a.getStatus() == AttemptStatus.PENDING_REVIEW);

            String status;
            if (now.isBefore(exam.getStartTime())) {
                status = "NOT_STARTED";
            } else if (!now.isBefore(exam.getEndTime())) {
                status = "ENDED";
            } else if (submitted >= exam.getMaxAttempts()) {
                status = "ATTEMPTS_USED";
            } else {
                status = "AVAILABLE";
            }

            Map<String, Object> item = new HashMap<>();
            item.put("examId", exam.getId());
            item.put("title", exam.getTitle());
            item.put("courseName", exam.getCourse().getCourseName());
            item.put("startTime", exam.getStartTime());
            item.put("endTime", exam.getEndTime());
            item.put("durationMinutes", exam.getDurationMinutes());
            item.put("maxAttempts", exam.getMaxAttempts());
            item.put("openBook", exam.getOpenBook());
            item.put("totalScore", exam.getPaper().getTotalScore());
            item.put("status", status);
            item.put("submittedAttempts", submitted);
            // 待阅卷：成绩未定，不展示分数
            item.put("grading", grading);
            if (grading) {
                item.put("score", null);
            } else if (!finished.isEmpty()) {
                // 已考成绩按成绩规则取（全部定稿后才有值）
                if (exam.getScoreRule() == com.example.app.entity.ScoreRule.BEST) {
                    item.put("score", finished.stream()
                            .map(ExamAttempt::getScore)
                            .filter(java.util.Objects::nonNull)
                            .max(java.math.BigDecimal::compareTo)
                            .orElse(null));
                } else {
                    item.put("score", finished.get(0).getScore());
                }
            } else {
                item.put("score", null);
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 学生开考：入场校验 → 新建或恢复 attempt → 返回题目与剩余时间
     */
    @Transactional
    public StartExamResponse start(Long examId) {
        Long studentId = currentStudentId();
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> BusinessException.notFound("考试不存在"));

        // 入场校验
        checkEnrollment(studentId, exam.getCourse().getId());
        LocalDateTime now = now();
        if (now.isBefore(exam.getStartTime())) {
            throw BusinessException.badRequest("考试尚未开始（" + exam.getStartTime() + " 开考）");
        }
        if (!now.isBefore(exam.getEndTime())) {
            throw BusinessException.badRequest("考试已截止");
        }

        long submitted = attemptRepository.countByExam_IdAndStudentIdAndStatus(
                examId, studentId, AttemptStatus.SUBMITTED);
        if (submitted >= exam.getMaxAttempts()) {
            throw BusinessException.badRequest("考试次数已用尽（已考 " + submitted + "/" + exam.getMaxAttempts() + " 次）");
        }

        // 续考：存在未过期的进行中尝试则恢复
        ExamAttempt attempt = attemptRepository
                .findByExam_IdAndStudentIdAndStatus(examId, studentId, AttemptStatus.IN_PROGRESS)
                .orElse(null);

        if (attempt != null) {
            if (now.isAfter(attempt.getDeadline())) {
                // 上次进行中已过截止，按已暂存答案强制交卷，本次新建
                forceSubmitAndSave(attempt);
                attempt = null;
            }
        }

        if (attempt == null) {
            attempt = new ExamAttempt();
            attempt.setExam(exam);
            attempt.setStudentId(studentId);
            attempt.setStatus(AttemptStatus.IN_PROGRESS);
            attempt.setStartTime(now);
            attempt.setDeadline(min(now.plusMinutes(exam.getDurationMinutes()), exam.getEndTime()));
            attempt.setViolationCount(0);
            attemptRepository.save(attempt);
        }

        long remaining = Duration.between(now, attempt.getDeadline()).getSeconds();

        List<ExamQuestionDTO> questions = buildQuestionList(exam.getPaper(), Boolean.TRUE.equals(exam.getShuffle()), exam.getOpenBook());
        return new StartExamResponse(
                attempt.getId(),
                exam.getId(),
                exam.getTitle(),
                exam.getOpenBook(),
                attempt.getDeadline(),
                remaining,
                questions,
                attempt.getAnswersJson()
        );
    }

    /**
     * 暂存答案；若已过截止则强制交卷
     */
    @Transactional
    public void saveAnswers(Long examId, Map<Long, String> answers) {
        ExamAttempt attempt = requireInProgress(examId, currentStudentId());
        if (now().isAfter(attempt.getDeadline())) {
            attempt.setAnswersJson(toJson(answers));
            forceSubmitAndSave(attempt);
            throw BusinessException.badRequest("考试已超时，已自动交卷");
        }
        attempt.setAnswersJson(toJson(answers));
        attemptRepository.save(attempt);
    }

    /**
     * 上报违规事件（记录性质，不强制交卷）
     */
    @Transactional
    public void reportViolation(Long examId, String type) {
        ExamAttempt attempt = requireInProgress(examId, currentStudentId());
        List<Map<String, Object>> violations = parseViolations(attempt.getViolationsJson());
        Map<String, Object> event = new HashMap<>();
        event.put("type", type == null ? "UNKNOWN" : type);
        event.put("time", now().toString());
        violations.add(event);
        attempt.setViolationsJson(toJson(violations));
        attempt.setViolationCount((attempt.getViolationCount() == null ? 0 : attempt.getViolationCount()) + 1);
        attemptRepository.save(attempt);
    }

    /**
     * 交卷 → 判分 → 返回本次得分（含主观题时返回待阅卷标记，成绩为客观题临时分）
     */
    @Transactional
    public Map<String, Object> submit(Long examId) {
        ExamAttempt attempt = requireInProgress(examId, currentStudentId());
        LocalDateTime now = now();
        // 服务端兜底：过截止按已暂存答案强制判分
        if (now.isAfter(attempt.getDeadline())) {
            attempt.setSubmitTime(attempt.getDeadline());
        } else {
            attempt.setSubmitTime(now);
        }
        attemptRepository.save(attempt);

        // grade 内部落明细、置尝试状态（SUBMITTED / PENDING_REVIEW）与成绩记录
        var result = gradingPort.grade(attempt);

        Map<String, Object> resp = new HashMap<>();
        resp.put("attemptId", attempt.getId());
        resp.put("score", result.score());
        resp.put("totalScore", attempt.getExam().getPaper().getTotalScore());
        resp.put("grading", result.pendingReview());
        return resp;
    }

    // ==================== 内部方法 ====================

    private Long currentStudentId() {
        UserContext.CurrentUser user = UserContext.get();
        if (user == null) {
            throw BusinessException.unauthorized("未登录");
        }
        SysUser sysUser = sysUserRepository.findById(user.userId())
                .orElseThrow(() -> BusinessException.unauthorized("用户账号不存在"));
        if (sysUser.getStudentId() == null) {
            throw BusinessException.forbidden("当前账号不是学生账号");
        }
        return sysUser.getStudentId();
    }

    private void checkEnrollment(Long studentId, Long courseId) {
        Enrollment en = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        if (en == null) {
            throw BusinessException.badRequest("未选修该课程，无法参加考试");
        }
    }

    private ExamAttempt requireInProgress(Long examId, Long studentId) {
        ExamAttempt attempt = attemptRepository
                .findByExam_IdAndStudentIdAndStatus(examId, studentId, AttemptStatus.IN_PROGRESS)
                .orElseThrow(() -> BusinessException.badRequest("没有进行中的考试，请先开考"));
        return attempt;
    }

    private void forceSubmitAndSave(ExamAttempt attempt) {
        attempt.setSubmitTime(attempt.getDeadline());
        attemptRepository.save(attempt);
        // grade 内部置状态（含主观题时为 PENDING_REVIEW）与分数
        gradingPort.grade(attempt);
    }

    /**
     * 组装题目列表：按 shuffle 决定是否乱序；按 open_book 决定是否下发答案与解析
     */
    private List<ExamQuestionDTO> buildQuestionList(ExamPaper paper, boolean shuffle, boolean openBook) {
        List<PaperQuestion> questions = paperQuestionRepository
                .findByPaper_IdOrderBySeqAsc(paper.getId());
        if (shuffle) {
            questions = new ArrayList<>(questions);
            Collections.shuffle(questions);
        }
        return questions.stream().map(pq -> {
            var qType = com.example.app.entity.QuestionType.parseOrNull(pq.getTypeSnapshot());
            boolean subjective = qType != null && qType.isSubjective();
            // 开卷下发答案：客观题为答案编码，主观题为参考答案；闭卷一律 null
            String answer = openBook
                    ? (subjective ? pq.getReferenceAnswerSnapshot() : pq.getAnswerSnapshot())
                    : null;
            return new ExamQuestionDTO(
                    pq.getId(),
                    pq.getTypeSnapshot(),
                    pq.getContentSnapshot(),
                    parseOptions(pq.getOptionsSnapshot()),
                    pq.getScore(),
                    answer,
                    openBook ? pq.getAnalysisSnapshot() : null
            );
        }).toList();
    }

    private List<QuestionOption> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOption>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> parseViolations(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {}));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw BusinessException.badRequest("数据序列化失败");
        }
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
