package com.example.app.service;

import com.example.app.entity.AttemptAnswer;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamRecord;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.QuestionType;
import com.example.app.entity.ScoreRule;
import com.example.app.repository.AttemptAnswerRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRecordRepository;
import com.example.app.repository.PaperQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 判分服务（SPEC-grading + M7 人工阅卷）
 * 交卷时 grade()（与交卷同事务）：
 * - 客观题逐题 AnswerMatcher 自动判分；
 * - 主观题（简答/论述）落 is_correct=3 待评阅，score=0 临时分，作答原文存 answerText；
 * - 尝试状态：含待评题 → PENDING_REVIEW，否则 SUBMITTED；
 * - 成绩记录按 exam.score_rule 从全部已结束尝试重算（BEST 取定稿最高分 / LAST 取末次），
 *   存在待阅尝试时记录置待定（pending=1，totalScore 为临时分）。
 * 教师评完所有主观题后 finalizeAttempt()：重算总分、尝试置 SUBMITTED、重算成绩记录。
 * 判分只读试卷快照，不实时读取题库；失败由调用方事务回滚，不产生半成品成绩。
 */
@Service
public class GradingService implements GradingPort {

    /** 主观题待评阅标记（SPEC-grading 预留 is_correct=3） */
    public static final int PENDING_REVIEW_FLAG = 3;

    private final PaperQuestionRepository paperQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ExamRecordRepository examRecordRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ObjectMapper objectMapper;

    public GradingService(PaperQuestionRepository paperQuestionRepository,
                          AttemptAnswerRepository attemptAnswerRepository,
                          ExamRecordRepository examRecordRepository,
                          ExamAttemptRepository examAttemptRepository,
                          ObjectMapper objectMapper) {
        this.paperQuestionRepository = paperQuestionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.examRecordRepository = examRecordRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public GradeResult grade(ExamAttempt attempt) {
        Map<Long, String> answers = parseAnswers(attempt.getAnswersJson());
        List<PaperQuestion> questions = paperQuestionRepository
                .findByPaper_IdOrderBySeqAsc(attempt.getExam().getPaper().getId());

        BigDecimal total = BigDecimal.ZERO;
        boolean pendingReview = false;
        List<AttemptAnswer> details = new ArrayList<>();
        for (PaperQuestion pq : questions) {
            String student = answers.get(pq.getId());
            QuestionType type = QuestionType.valueOf(pq.getTypeSnapshot());

            AttemptAnswer aa = new AttemptAnswer();
            aa.setAttempt(attempt);
            aa.setPaperQuestion(pq);
            aa.setQuestionType(pq.getTypeSnapshot());
            aa.setFullScore(pq.getScore());

            if (type.isSubjective()) {
                // 主观题：不自动判分，落待评阅；作答原文存 answerText
                String text = student == null || student.isBlank() ? null : student.trim();
                aa.setStudentAnswer(null);
                aa.setAnswerText(text);
                aa.setCorrectAnswer("");
                aa.setIsCorrect(PENDING_REVIEW_FLAG);
                aa.setScore(BigDecimal.ZERO);
                pendingReview = true;
            } else {
                var result = AnswerMatcher.match(type, student, pq.getAnswerSnapshot(), pq.getScore());
                aa.setStudentAnswer(student == null || student.isBlank() ? null : student.trim());
                aa.setAnswerText(null);
                aa.setCorrectAnswer(pq.getAnswerSnapshot());
                aa.setIsCorrect(result.correct() ? 1 : 0);
                aa.setScore(result.score());
                total = total.add(result.score());
            }
            details.add(aa);
        }
        attemptAnswerRepository.saveAll(details);

        attempt.setScore(total);
        attempt.setStatus(pendingReview ? AttemptStatus.PENDING_REVIEW : AttemptStatus.SUBMITTED);
        examAttemptRepository.save(attempt);

        recomputeRecord(attempt);
        return new GradeResult(total, pendingReview);
    }

    @Override
    @Transactional
    public BigDecimal finalizeAttempt(ExamAttempt attempt) {
        // 安全校验：仍有未评主观题不允许定稿
        long pending = attemptAnswerRepository.countByAttempt_IdAndIsCorrect(
                attempt.getId(), PENDING_REVIEW_FLAG);
        if (pending > 0) {
            throw new IllegalStateException("该尝试仍有 " + pending + " 道题待评阅，无法定稿成绩");
        }

        BigDecimal total = attemptAnswerRepository
                .findByAttempt_IdOrderByPaperQuestion_IdAsc(attempt.getId()).stream()
                .map(AttemptAnswer::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setScore(total);
        examAttemptRepository.save(attempt);

        recomputeRecord(attempt);
        return total;
    }

    /**
     * 从该生本场考试的全部已结束尝试重算成绩记录（每次判分/定稿后调用，避免增量状态错误）：
     * - attemptCount = SUBMITTED + PENDING_REVIEW 尝试数（交卷即占用次数）；
     * - pending = 存在 PENDING_REVIEW 尝试；
     * - LAST：有效成绩取最近一次已结束尝试；
     * - BEST：取定稿（SUBMITTED）尝试最高分；无定稿时取待阅尝试临时分（记录保持待定）。
     */
    private void recomputeRecord(ExamAttempt attempt) {
        Exam exam = attempt.getExam();
        Long studentId = attempt.getStudentId();
        List<ExamAttempt> all = examAttemptRepository
                .findByExam_IdAndStudentIdOrderByStartTimeDesc(exam.getId(), studentId);
        List<ExamAttempt> finished = all.stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .toList();
        if (finished.isEmpty()) {
            return;
        }
        boolean pendingAny = finished.stream()
                .anyMatch(a -> a.getStatus() == AttemptStatus.PENDING_REVIEW);
        List<ExamAttempt> finalized = finished.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .toList();

        ExamRecord record = examRecordRepository.findByExam_IdAndStudentId(exam.getId(), studentId)
                .orElseGet(ExamRecord::new);
        if (record.getId() == null) {
            record.setExam(exam);
            record.setStudentId(studentId);
        }
        record.setAttemptCount(finished.size());
        record.setPending(pendingAny ? 1 : 0);

        ExamAttempt last = finished.get(0); // 已按开考时间倒序
        record.setLastAttemptId(last.getId());

        ExamAttempt effective;
        if (exam.getScoreRule() == ScoreRule.BEST) {
            List<ExamAttempt> pool = finalized.isEmpty() ? finished : finalized;
            effective = pool.stream()
                    .max(Comparator.comparing(ExamAttempt::getScore,
                            Comparator.nullsFirst(BigDecimal::compareTo)))
                    .orElse(last);
        } else {
            effective = last;
        }
        record.setTotalScore(effective.getScore() == null ? BigDecimal.ZERO : effective.getScore());
        record.setBestAttemptId(effective.getId());
        examRecordRepository.save(record);
    }

    private Map<Long, String> parseAnswers(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<Long, String>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
