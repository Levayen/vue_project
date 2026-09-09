package com.example.app.service;

import com.example.app.entity.AttemptAnswer;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.ExamRecord;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.ScoreRule;
import com.example.app.repository.AttemptAnswerRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRecordRepository;
import com.example.app.repository.PaperQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GradingService 单元测试（SPEC-grading + M7 人工阅卷）
 * 覆盖：逐题落库 attempt_answer、未答题标记、总分汇总、LAST/BEST 成绩聚合、
 * 主观题待评阅（is_correct=3 / PENDING_REVIEW / record.pending）、教师评完后定稿。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GradingServiceTest {

    @Mock
    private PaperQuestionRepository paperQuestionRepository;
    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;
    @Mock
    private ExamRecordRepository examRecordRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GradingService gradingService;

    private Exam exam;
    private ExamAttempt attempt;

    @BeforeEach
    void setUp() {
        gradingService = new GradingService(paperQuestionRepository, attemptAnswerRepository,
                examRecordRepository, examAttemptRepository, objectMapper);

        ExamPaper paper = new ExamPaper();
        paper.setId(100L);
        exam = new Exam();
        exam.setId(10L);
        exam.setPaper(paper);
        exam.setScoreRule(ScoreRule.LAST);

        attempt = new ExamAttempt();
        attempt.setId(1L);
        attempt.setExam(exam);
        attempt.setStudentId(5L);

        // 成绩重算默认只回显当前尝试（LAST）
        when(examAttemptRepository.findByExam_IdAndStudentIdOrderByStartTimeDesc(10L, 5L))
                .thenReturn(List.of(attempt));
    }

    private PaperQuestion pq(long id, String type, String answer, String score) {
        PaperQuestion q = new PaperQuestion();
        q.setId(id);
        q.setTypeSnapshot(type);
        q.setAnswerSnapshot(answer);
        q.setScore(new BigDecimal(score));
        return q;
    }

    private void mockPaper(PaperQuestion... questions) {
        when(paperQuestionRepository.findByPaper_IdOrderBySeqAsc(100L)).thenReturn(List.of(questions));
    }

    private void mockRecord(ExamRecord record) {
        when(examRecordRepository.findByExam_IdAndStudentId(10L, 5L))
                .thenReturn(Optional.ofNullable(record));
    }

    @SuppressWarnings("unchecked")
    private <T> ArgumentCaptor<T> captor(Class<?> raw) {
        return ArgumentCaptor.forClass((Class<T>) raw);
    }

    @Test
    void gradeWritesDetailRowsAndReturnsTotal() {
        mockPaper(
                pq(1L, "SINGLE", "A", "2.5"),
                pq(2L, "MULTI", "ABD", "4"),
                pq(3L, "JUDGE", "T", "2"),
                pq(4L, "FILL", "北京||北京市", "1.5")
        );
        attempt.setAnswersJson("{\"1\":\"A\",\"2\":\"ABC\",\"3\":\"T\",\"4\":\"北京市\"}");

        GradingPort.GradeResult result = gradingService.grade(attempt);

        assertEquals(new BigDecimal("6.0"), result.score());
        assertFalse(result.pendingReview());
        ArgumentCaptor<List<AttemptAnswer>> captor = captor(List.class);
        verify(attemptAnswerRepository).saveAll(captor.capture());
        List<AttemptAnswer> rows = captor.getValue();
        assertEquals(4, rows.size());

        AttemptAnswer single = rows.get(0);
        assertEquals("A", single.getStudentAnswer());
        assertEquals("A", single.getCorrectAnswer());
        assertEquals(1, single.getIsCorrect());
        assertEquals(new BigDecimal("2.5"), single.getScore());

        // 多选错选 → 0 分
        AttemptAnswer multi = rows.get(1);
        assertEquals(0, multi.getIsCorrect());
        assertEquals(BigDecimal.ZERO, multi.getScore());

        AttemptAnswer judge = rows.get(2);
        assertEquals(1, judge.getIsCorrect());

        // 填空命中第二可接受答案
        AttemptAnswer fill = rows.get(3);
        assertEquals(1, fill.getIsCorrect());
        assertEquals(new BigDecimal("1.5"), fill.getScore());
    }

    @Test
    void unansweredMarkedZero() {
        mockPaper(pq(1L, "SINGLE", "A", "2.5"), pq(2L, "FILL", "北京", "2"));
        attempt.setAnswersJson("{\"1\":\"A\"}");

        GradingPort.GradeResult result = gradingService.grade(attempt);

        assertEquals(new BigDecimal("2.5"), result.score());
        ArgumentCaptor<List<AttemptAnswer>> captor = captor(List.class);
        verify(attemptAnswerRepository).saveAll(captor.capture());
        AttemptAnswer unanswered = captor.getValue().get(1);
        assertNull(unanswered.getStudentAnswer());
        assertEquals(0, unanswered.getIsCorrect());
        assertEquals(BigDecimal.ZERO, unanswered.getScore());
        assertEquals(new BigDecimal("2"), unanswered.getFullScore());
    }

    @Test
    void lastRuleUpsertsRecord() {
        mockPaper(pq(1L, "SINGLE", "A", "3"));
        attempt.setAnswersJson("{\"1\":\"A\"}");
        mockRecord(null);

        gradingService.grade(attempt);

        ArgumentCaptor<ExamRecord> captor = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRecordRepository).save(captor.capture());
        ExamRecord record = captor.getValue();
        assertEquals(0, record.getTotalScore().compareTo(new BigDecimal("3.0")));
        assertEquals(1L, record.getLastAttemptId());
        assertEquals(1, record.getAttemptCount());
        assertEquals(0, record.getPending());
        assertEquals(5L, record.getStudentId());
        assertSame(exam, record.getExam());
    }

    @Test
    void bestRuleKeepsHighestScore() {
        exam.setScoreRule(ScoreRule.BEST);
        mockPaper(pq(1L, "SINGLE", "A", "3"));
        attempt.setAnswersJson("{\"1\":\"A\"}");

        // 历史定稿尝试 8 分
        ExamAttempt past = new ExamAttempt();
        past.setId(2L);
        past.setExam(exam);
        past.setStudentId(5L);
        past.setStatus(AttemptStatus.SUBMITTED);
        past.setScore(new BigDecimal("8"));
        when(examAttemptRepository.findByExam_IdAndStudentIdOrderByStartTimeDesc(10L, 5L))
                .thenReturn(List.of(attempt, past));

        ExamRecord existing = new ExamRecord();
        existing.setId(9L);
        existing.setExam(exam);
        existing.setStudentId(5L);
        existing.setTotalScore(new BigDecimal("8"));
        existing.setBestAttemptId(2L);
        existing.setAttemptCount(1);
        mockRecord(existing);

        gradingService.grade(attempt);

        ArgumentCaptor<ExamRecord> captor = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRecordRepository).save(captor.capture());
        ExamRecord record = captor.getValue();
        // 本次 3 分低于历史 8 分，有效成绩保持 8，best 仍指向 attempt 2
        assertEquals(new BigDecimal("8"), record.getTotalScore());
        assertEquals(2L, record.getBestAttemptId());
        assertEquals(1L, record.getLastAttemptId());
        assertEquals(2, record.getAttemptCount());
        assertEquals(0, record.getPending());
    }

    @Test
    void bestRuleUpdatesWhenHigher() {
        exam.setScoreRule(ScoreRule.BEST);
        mockPaper(pq(1L, "SINGLE", "A", "3"));
        attempt.setAnswersJson("{\"1\":\"A\"}");

        ExamAttempt past = new ExamAttempt();
        past.setId(2L);
        past.setExam(exam);
        past.setStudentId(5L);
        past.setStatus(AttemptStatus.SUBMITTED);
        past.setScore(new BigDecimal("1.5"));
        when(examAttemptRepository.findByExam_IdAndStudentIdOrderByStartTimeDesc(10L, 5L))
                .thenReturn(List.of(attempt, past));

        ExamRecord existing = new ExamRecord();
        existing.setId(9L);
        existing.setExam(exam);
        existing.setStudentId(5L);
        existing.setTotalScore(new BigDecimal("1.5"));
        existing.setBestAttemptId(2L);
        existing.setAttemptCount(1);
        mockRecord(existing);

        gradingService.grade(attempt);

        ArgumentCaptor<ExamRecord> captor = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRecordRepository).save(captor.capture());
        ExamRecord record = captor.getValue();
        assertEquals(0, record.getTotalScore().compareTo(new BigDecimal("3.0")));
        assertEquals(1L, record.getBestAttemptId());
        assertEquals(1L, record.getLastAttemptId());
    }

    @Test
    void subjectiveQuestionGoesPendingReview() {
        mockPaper(
                pq(1L, "SINGLE", "A", "2.5"),
                pq(5L, "SHORT_ANSWER", "", "5")
        );
        attempt.setAnswersJson("{\"1\":\"A\",\"5\":\"这是我的简答题作答\"}");
        mockRecord(null);

        GradingPort.GradeResult result = gradingService.grade(attempt);

        // 临时分仅含客观题 2.5，待阅卷
        assertEquals(new BigDecimal("2.5"), result.score());
        assertTrue(result.pendingReview());
        assertEquals(AttemptStatus.PENDING_REVIEW, attempt.getStatus());

        ArgumentCaptor<List<AttemptAnswer>> captor = captor(List.class);
        verify(attemptAnswerRepository).saveAll(captor.capture());
        AttemptAnswer subjective = captor.getValue().get(1);
        assertEquals(3, subjective.getIsCorrect());
        assertEquals(BigDecimal.ZERO, subjective.getScore());
        assertEquals("这是我的简答题作答", subjective.getAnswerText());
        assertNull(subjective.getStudentAnswer());
        assertEquals("", subjective.getCorrectAnswer());

        // 成绩记录待定：临时分 2.5，pending=1
        ArgumentCaptor<ExamRecord> rc = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRecordRepository).save(rc.capture());
        ExamRecord record = rc.getValue();
        assertEquals(1, record.getPending());
        assertEquals(0, record.getTotalScore().compareTo(new BigDecimal("2.5")));
        assertEquals(1, record.getAttemptCount());
    }

    @Test
    void subjectiveUnansweredStillPendingWithNullText() {
        mockPaper(pq(5L, "ESSAY", "", "10"));
        attempt.setAnswersJson("{}");

        GradingPort.GradeResult result = gradingService.grade(attempt);

        assertEquals(BigDecimal.ZERO, result.score());
        assertTrue(result.pendingReview());
        ArgumentCaptor<List<AttemptAnswer>> captor = captor(List.class);
        verify(attemptAnswerRepository).saveAll(captor.capture());
        AttemptAnswer subjective = captor.getValue().get(0);
        assertEquals(3, subjective.getIsCorrect());
        assertNull(subjective.getAnswerText());
    }

    @Test
    void finalizeAttemptRecomputesTotalAndClearsPending() {
        mockPaper(
                pq(1L, "SINGLE", "A", "2.5"),
                pq(5L, "SHORT_ANSWER", "", "5")
        );
        attempt.setAnswersJson("{\"1\":\"A\",\"5\":\"作答\"}");
        mockRecord(null);

        GradingPort.GradeResult result = gradingService.grade(attempt);
        assertTrue(result.pendingReview());

        // 教师评分：客观行 2.5 + 主观行满分 5
        AttemptAnswer objective = new AttemptAnswer();
        objective.setScore(new BigDecimal("2.5"));
        AttemptAnswer subjective = new AttemptAnswer();
        subjective.setScore(new BigDecimal("5"));
        when(attemptAnswerRepository.findByAttempt_IdOrderByPaperQuestion_IdAsc(1L))
                .thenReturn(List.of(objective, subjective));
        when(attemptAnswerRepository.countByAttempt_IdAndIsCorrect(1L, 3)).thenReturn(0L);

        BigDecimal finalScore = gradingService.finalizeAttempt(attempt);

        assertEquals(0, finalScore.compareTo(new BigDecimal("7.5")));
        assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
        assertEquals(0, finalScore.compareTo(attempt.getScore()));

        // 定稿后重算记录：pending=0、总分 7.5
        ArgumentCaptor<ExamRecord> rc = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRecordRepository, atLeastOnce()).save(rc.capture());
        ExamRecord record = rc.getValue();
        assertEquals(0, record.getPending());
        assertEquals(0, record.getTotalScore().compareTo(new BigDecimal("7.5")));
    }

    @Test
    void finalizeAttemptRejectedWhileQuestionsPending() {
        when(attemptAnswerRepository.countByAttempt_IdAndIsCorrect(1L, 3)).thenReturn(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> gradingService.finalizeAttempt(attempt));
        assertTrue(ex.getMessage().contains("待评阅"));
        verify(examRecordRepository, never()).save(any());
    }

    @Test
    void bestRuleWithPendingAttemptDoesNotPolluteFinalScore() {
        exam.setScoreRule(ScoreRule.BEST);
        // 当前尝试含主观题：grade 后 PENDING_REVIEW、临时分 0
        mockPaper(pq(5L, "SHORT_ANSWER", "", "2.5"));
        attempt.setAnswersJson("{}");

        ExamAttempt past = new ExamAttempt();
        past.setId(2L);
        past.setExam(exam);
        past.setStudentId(5L);
        past.setStatus(AttemptStatus.SUBMITTED);
        past.setScore(new BigDecimal("8"));
        when(examAttemptRepository.findByExam_IdAndStudentIdOrderByStartTimeDesc(10L, 5L))
                .thenReturn(List.of(attempt, past));
        mockRecord(null);

        gradingService.grade(attempt);

        ArgumentCaptor<ExamRecord> rc = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRecordRepository).save(rc.capture());
        ExamRecord record = rc.getValue();
        // 记录待定，但 BEST 有效成绩取定稿池最高 8 分（不被待阅临时分污染）
        assertEquals(1, record.getPending());
        assertEquals(new BigDecimal("8"), record.getTotalScore());
        assertEquals(2L, record.getBestAttemptId());
        assertEquals(1L, record.getLastAttemptId());
    }
}
