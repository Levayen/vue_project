package com.example.app.service;

import com.example.app.dto.PendingAttemptDTO;
import com.example.app.dto.PendingExamDTO;
import com.example.app.dto.ReviewAttemptDTO;
import com.example.app.dto.ScoreRequest;
import com.example.app.entity.AttemptAnswer;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.QuestionType;
import com.example.app.entity.Student;
import com.example.app.exception.BusinessException;
import com.example.app.repository.AttemptAnswerRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 人工阅卷服务（M7：主观题简答/论述）
 * 教师阅卷工作台：
 * - pendingExams()：所有存在待评答卷的考试汇总；
 * - pendingAttempts(examId)：某场考试的待评学生列表；
 * - reviewAttempt(attemptId)：一份答卷的全部主观题（题干/参考答案/学生作答/得分/评语）；
 * - scoreAnswer(answerId, req)：逐题评分，全部评完后自动定稿（GradingPort.finalizeAttempt）。
 * 权限由 /api/teacher/** 路径拦截限定 TEACHER/ADMIN。
 */
@Service
public class ReviewService {

    private static final int PENDING_REVIEW_FLAG = GradingService.PENDING_REVIEW_FLAG;

    private final ExamAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final StudentRepository studentRepository;
    private final GradingPort gradingPort;

    public ReviewService(ExamAttemptRepository attemptRepository,
                         AttemptAnswerRepository answerRepository,
                         StudentRepository studentRepository,
                         GradingPort gradingPort) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.studentRepository = studentRepository;
        this.gradingPort = gradingPort;
    }

    /**
     * 有待评答卷的考试列表（按交卷时间倒序聚合）
     */
    @Transactional(readOnly = true)
    public List<PendingExamDTO> pendingExams() {
        List<ExamAttempt> pending = attemptRepository.findByStatus(AttemptStatus.PENDING_REVIEW);
        Map<Long, List<ExamAttempt>> byExam = new LinkedHashMap<>();
        for (ExamAttempt a : pending) {
            byExam.computeIfAbsent(a.getExam().getId(), k -> new ArrayList<>()).add(a);
        }
        List<PendingExamDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<ExamAttempt>> e : byExam.entrySet()) {
            List<ExamAttempt> attempts = e.getValue();
            Exam exam = attempts.get(0).getExam();
            long questionCount = attempts.stream()
                    .mapToLong(a -> answerRepository.countByAttempt_IdAndIsCorrect(
                            a.getId(), PENDING_REVIEW_FLAG))
                    .sum();
            result.add(new PendingExamDTO(
                    exam.getId(),
                    exam.getTitle(),
                    exam.getCourse().getCourseName(),
                    (long) attempts.size(),
                    questionCount
            ));
        }
        return result;
    }

    /**
     * 某场考试的待评学生答卷列表
     */
    @Transactional(readOnly = true)
    public List<PendingAttemptDTO> pendingAttempts(Long examId) {
        List<ExamAttempt> list = attemptRepository
                .findByExam_IdAndStatus(examId, AttemptStatus.PENDING_REVIEW);
        return list.stream()
                .sorted(Comparator.comparing(ExamAttempt::getSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(a -> {
                    Student s = studentRepository.findById(a.getStudentId()).orElse(null);
                    return new PendingAttemptDTO(
                            a.getId(),
                            a.getStudentId(),
                            s == null ? "-" : s.getStudentNumber(),
                            s == null ? "-" : s.getName(),
                            s == null || s.getClassInfo() == null ? "-" : s.getClassInfo().getClassName(),
                            answerRepository.countByAttempt_IdAndIsCorrect(a.getId(), PENDING_REVIEW_FLAG),
                            a.getScore(),
                            a.getSubmitTime()
                    );
                }).toList();
    }

    /**
     * 一份答卷的主观题评分详情（已交卷即可看；包含待评与已评）
     */
    @Transactional(readOnly = true)
    public ReviewAttemptDTO reviewAttempt(Long attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> BusinessException.notFound("答卷不存在"));
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw BusinessException.badRequest("该答卷尚未交卷，不能阅卷");
        }
        Exam exam = attempt.getExam();
        Student student = studentRepository.findById(attempt.getStudentId()).orElse(null);

        List<ReviewAttemptDTO.SubjectiveAnswerDTO> answers = answerRepository
                .findByAttempt_IdOrderByPaperQuestion_IdAsc(attemptId).stream()
                .filter(aa -> QuestionType.parseOrNull(aa.getQuestionType()) != null
                        && QuestionType.valueOf(aa.getQuestionType()).isSubjective())
                .map(aa -> new ReviewAttemptDTO.SubjectiveAnswerDTO(
                        aa.getId(),
                        aa.getQuestionType(),
                        aa.getPaperQuestion().getContentSnapshot(),
                        aa.getPaperQuestion().getReferenceAnswerSnapshot(),
                        aa.getAnswerText(),
                        aa.getFullScore(),
                        aa.getIsCorrect() != null && aa.getIsCorrect() == PENDING_REVIEW_FLAG
                                ? null : aa.getScore(),
                        aa.getIsCorrect(),
                        aa.getTeacherComment()
                )).toList();

        return new ReviewAttemptDTO(
                attempt.getId(),
                exam.getId(),
                exam.getTitle(),
                student == null ? "-" : student.getStudentNumber(),
                student == null ? "-" : student.getName(),
                student == null || student.getClassInfo() == null ? "-" : student.getClassInfo().getClassName(),
                exam.getPaper().getTotalScore(),
                attempt.getScore() == null ? BigDecimal.ZERO : attempt.getScore(),
                attempt.getStatus() == AttemptStatus.SUBMITTED,
                answers
        );
    }

    /**
     * 评阅单道主观题：设置得分与评语；全部评完自动定稿并返回结果。
     * 得分口径：得分 == 满分记正确（1），否则记 0（部分分可入错题本）。
     *
     * @return [剩余待评题数, 是否触发定稿]
     */
    @Transactional
    public Map<String, Object> scoreAnswer(Long answerId, ScoreRequest req) {
        if (req == null || req.score() == null) {
            throw BusinessException.badRequest("得分不能为空");
        }
        BigDecimal score = req.score();
        AttemptAnswer aa = answerRepository.findById(answerId)
                .orElseThrow(() -> BusinessException.notFound("题目作答不存在"));
        QuestionType type = QuestionType.parseOrNull(aa.getQuestionType());
        if (type == null || !type.isSubjective()) {
            throw BusinessException.badRequest("该题不是主观题，不能人工评分");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(aa.getFullScore()) > 0) {
            throw BusinessException.badRequest(
                    "得分必须在 0 ~ " + aa.getFullScore().stripTrailingZeros().toPlainString() + " 之间");
        }

        aa.setScore(score);
        aa.setIsCorrect(score.compareTo(aa.getFullScore()) == 0 ? 1 : 0);
        aa.setTeacherComment(req.comment() == null || req.comment().isBlank()
                ? null : req.comment().trim());
        answerRepository.save(aa);

        ExamAttempt attempt = aa.getAttempt();
        long remaining = answerRepository.countByAttempt_IdAndIsCorrect(
                attempt.getId(), PENDING_REVIEW_FLAG);
        boolean finalized = false;
        if (remaining == 0) {
            // 最后一道评完（或教师改分后无待评）→ 重算总分、定稿、重算成绩记录
            gradingPort.finalizeAttempt(attempt);
            finalized = true;
        }
        return Map.of("remaining", remaining, "finalized", finalized);
    }
}
