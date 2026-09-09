package com.example.app.service;

import com.example.app.dto.ExamReviewDTO;
import com.example.app.dto.QuestionOption;
import com.example.app.dto.QuestionReviewDTO;
import com.example.app.dto.StudentResultDTO;
import com.example.app.dto.WrongQuestionDTO;
import com.example.app.entity.AttemptAnswer;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamRecord;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.ScoreRule;
import com.example.app.entity.SysUser;
import com.example.app.exception.BusinessException;
import com.example.app.repository.AttemptAnswerRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRecordRepository;
import com.example.app.repository.SysUserRepository;
import com.example.app.security.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生成绩服务（SPEC-results）
 * 成绩列表、单次考试回顾（交卷后开放答案与解析）、跨考试错题本。
 * 身份取自 token，仅允许访问本人数据（attempt.studentId 校验，越权 403）。
 * 未交卷（无有效成绩）访问回顾 → 404。
 */
@Service
public class ResultService {

    /** 及格线常量（SPEC-results 统计口径） */
    public static final BigDecimal PASS_SCORE = new BigDecimal("60");

    private final ExamRecordRepository examRecordRepository;
    private final ExamAttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final SysUserRepository sysUserRepository;
    private final ObjectMapper objectMapper;

    public ResultService(ExamRecordRepository examRecordRepository,
                         ExamAttemptRepository attemptRepository,
                         AttemptAnswerRepository attemptAnswerRepository,
                         SysUserRepository sysUserRepository,
                         ObjectMapper objectMapper) {
        this.examRecordRepository = examRecordRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.sysUserRepository = sysUserRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 我的成绩列表（按更新时间倒序）
     */
    @Transactional(readOnly = true)
    public List<StudentResultDTO> myResults() {
        Long studentId = currentStudentId();
        return examRecordRepository.findByStudentIdOrderByUpdateTimeDesc(studentId).stream()
                .map(this::toResultDTO)
                .toList();
    }

    /**
     * 单次考试回顾：逐题我的答案/正确答案/对错/解析（闭卷考试交卷后开放）
     */
    @Transactional(readOnly = true)
    public ExamReviewDTO review(Long examId) {
        Long studentId = currentStudentId();
        ExamRecord record = examRecordRepository.findByExam_IdAndStudentId(examId, studentId)
                .orElseThrow(() -> BusinessException.notFound("未交卷或无有效成绩，暂无回顾内容"));

        ExamAttempt attempt = requireOwnedAttempt(record, studentId);
        Exam exam = attempt.getExam();

        List<QuestionReviewDTO> questions = attemptAnswerRepository
                .findByAttempt_IdOrderByPaperQuestion_IdAsc(attempt.getId()).stream()
                .map(this::toReviewDTO)
                .toList();

        return new ExamReviewDTO(
                examId,
                exam.getTitle(),
                exam.getCourse().getCourseName(),
                record.getTotalScore(),
                exam.getPaper().getTotalScore(),
                attempt.getSubmitTime(),
                questions
        );
    }

    /**
     * 错题本：跨考试聚合本人已交卷尝试中 is_correct=0 的题目，可按课程筛选
     */
    @Transactional(readOnly = true)
    public List<WrongQuestionDTO> wrongBook(Long courseId) {
        Long studentId = currentStudentId();
        List<AttemptAnswer> details = courseId == null
                ? attemptAnswerRepository.findByAttempt_StudentIdAndAttempt_StatusAndIsCorrect(
                        studentId, AttemptStatus.SUBMITTED, 0)
                : attemptAnswerRepository.findByAttempt_StudentIdAndAttempt_StatusAndIsCorrectAndAttempt_Exam_Course_Id(
                        studentId, AttemptStatus.SUBMITTED, 0, courseId);
        return details.stream()
                .map(this::toWrongDTO)
                .toList();
    }

    // ==================== 内部方法 ====================

    private StudentResultDTO toResultDTO(ExamRecord record) {
        Exam exam = record.getExam();
        ExamAttempt attempt = resolveValidAttempt(record);
        BigDecimal score = record.getTotalScore() == null ? BigDecimal.ZERO : record.getTotalScore();
        // M7：成绩待定（含待阅主观题）时不判及格，前端展示"待阅卷"
        boolean pending = record.getPending() != null && record.getPending() == 1;
        return new StudentResultDTO(
                exam.getId(),
                exam.getTitle(),
                exam.getCourse().getCourseName(),
                score,
                record.getAttemptCount(),
                attempt == null ? null : attempt.getSubmitTime(),
                !pending && score.compareTo(PASS_SCORE) >= 0,
                pending
        );
    }

    /**
     * 有效成绩对应的尝试：LAST 取末次、BEST 取最高
     */
    private ExamAttempt resolveValidAttempt(ExamRecord record) {
        Long attemptId = record.getExam().getScoreRule() == ScoreRule.BEST
                ? record.getBestAttemptId()
                : record.getLastAttemptId();
        if (attemptId == null) {
            attemptId = record.getBestAttemptId() != null ? record.getBestAttemptId() : record.getLastAttemptId();
        }
        return attemptId == null ? null : attemptRepository.findById(attemptId).orElse(null);
    }

    /**
     * 回顾仅允许本人数据；数据归属校验失败 → 403
     */
    private ExamAttempt requireOwnedAttempt(ExamRecord record, Long studentId) {
        ExamAttempt attempt = resolveValidAttempt(record);
        if (attempt == null) {
            throw BusinessException.notFound("无已交卷的尝试，暂无回顾内容");
        }
        if (!attempt.getStudentId().equals(studentId)) {
            throw BusinessException.forbidden("只能查看本人考试的回顾");
        }
        return attempt;
    }

    private QuestionReviewDTO toReviewDTO(AttemptAnswer aa) {
        PaperQuestion pq = aa.getPaperQuestion();
        var qType = com.example.app.entity.QuestionType.parseOrNull(pq.getTypeSnapshot());
        boolean subjective = qType != null && qType.isSubjective();
        return new QuestionReviewDTO(
                pq.getTypeSnapshot(),
                pq.getContentSnapshot(),
                parseOptions(pq.getOptionsSnapshot()),
                // 主观题学生作答取长文本字段
                subjective ? aa.getAnswerText() : aa.getStudentAnswer(),
                aa.getCorrectAnswer(),
                aa.getIsCorrect(),
                pq.getAnalysisSnapshot(),
                aa.getScore(),
                aa.getFullScore(),
                subjective ? pq.getReferenceAnswerSnapshot() : null,
                aa.getTeacherComment()
        );
    }

    private WrongQuestionDTO toWrongDTO(AttemptAnswer aa) {
        PaperQuestion pq = aa.getPaperQuestion();
        ExamAttempt attempt = aa.getAttempt();
        Exam exam = attempt.getExam();
        boolean subjective = com.example.app.entity.QuestionType
                .valueOf(pq.getTypeSnapshot()).isSubjective();
        return new WrongQuestionDTO(
                exam.getId(),
                exam.getTitle(),
                exam.getCourse().getCourseName(),
                pq.getTypeSnapshot(),
                pq.getContentSnapshot(),
                parseOptions(pq.getOptionsSnapshot()),
                subjective ? aa.getAnswerText() : aa.getStudentAnswer(),
                aa.getCorrectAnswer(),
                pq.getAnalysisSnapshot(),
                aa.getScore(),
                aa.getFullScore(),
                attempt.getSubmitTime(),
                subjective ? pq.getReferenceAnswerSnapshot() : null
        );
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
}
