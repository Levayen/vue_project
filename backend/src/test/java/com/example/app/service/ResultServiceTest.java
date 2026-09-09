package com.example.app.service;

import com.example.app.dto.ExamReviewDTO;
import com.example.app.dto.StudentResultDTO;
import com.example.app.dto.WrongQuestionDTO;
import com.example.app.entity.AttemptAnswer;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Course;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.ExamRecord;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.ScoreRule;
import com.example.app.entity.SysUser;
import com.example.app.entity.UserRole;
import com.example.app.exception.BusinessException;
import com.example.app.repository.AttemptAnswerRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRecordRepository;
import com.example.app.repository.SysUserRepository;
import com.example.app.security.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * ResultService 单元测试（SPEC-results）
 * 覆盖：成绩列表及格判定、未交卷回顾 404、越权回顾 403、错题本只聚合 is_correct=0。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long STUDENT_ID = 5L;
    private static final Long EXAM_ID = 10L;

    @Mock
    private ExamRecordRepository examRecordRepository;
    @Mock
    private ExamAttemptRepository attemptRepository;
    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;
    @Mock
    private SysUserRepository sysUserRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResultService resultService;

    private Exam exam;
    private ExamAttempt attempt;

    @BeforeEach
    void setUp() {
        resultService = new ResultService(examRecordRepository, attemptRepository,
                attemptAnswerRepository, sysUserRepository, objectMapper);
        UserContext.set(new UserContext.CurrentUser(USER_ID, "student01", UserRole.STUDENT));

        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setStudentId(STUDENT_ID);
        when(sysUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Course course = new Course(1L, "高等数学", null, null);
        ExamPaper paper = new ExamPaper();
        paper.setId(100L);
        paper.setTotalScore(new BigDecimal("10"));
        exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setTitle("期中考试");
        exam.setCourse(course);
        exam.setPaper(paper);
        exam.setScoreRule(ScoreRule.LAST);

        attempt = new ExamAttempt();
        attempt.setId(2L);
        attempt.setExam(exam);
        attempt.setStudentId(STUDENT_ID);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmitTime(LocalDateTime.of(2026, 9, 8, 12, 0));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private ExamRecord record(String score) {
        ExamRecord r = new ExamRecord();
        r.setId(9L);
        r.setExam(exam);
        r.setStudentId(STUDENT_ID);
        r.setTotalScore(new BigDecimal(score));
        r.setLastAttemptId(2L);
        r.setAttemptCount(1);
        return r;
    }

    @Test
    void myResultsMapsPassFlag() {
        when(examRecordRepository.findByStudentIdOrderByUpdateTimeDesc(STUDENT_ID))
                .thenReturn(List.of(record("75"), record("40")));
        when(attemptRepository.findById(2L)).thenReturn(Optional.of(attempt));

        List<StudentResultDTO> results = resultService.myResults();

        assertEquals(2, results.size());
        assertTrue(results.get(0).pass());
        assertFalse(results.get(1).pass());
        assertEquals("期中考试", results.get(0).examTitle());
        assertEquals("高等数学", results.get(0).courseName());
        assertEquals(LocalDateTime.of(2026, 9, 8, 12, 0), results.get(0).submitTime());
    }

    @Test
    void reviewWithoutRecordReturns404() {
        when(examRecordRepository.findByExam_IdAndStudentId(EXAM_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> resultService.review(EXAM_ID));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void reviewOtherStudentsAttemptReturns403() {
        ExamRecord r = record("75");
        when(examRecordRepository.findByExam_IdAndStudentId(EXAM_ID, STUDENT_ID))
                .thenReturn(Optional.of(r));
        attempt.setStudentId(999L); // 他人尝试
        when(attemptRepository.findById(2L)).thenReturn(Optional.of(attempt));

        BusinessException ex = assertThrows(BusinessException.class, () -> resultService.review(EXAM_ID));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void reviewBuildsQuestionsWithAnalysis() {
        ExamRecord r = record("5");
        when(examRecordRepository.findByExam_IdAndStudentId(EXAM_ID, STUDENT_ID))
                .thenReturn(Optional.of(r));
        when(attemptRepository.findById(2L)).thenReturn(Optional.of(attempt));

        AttemptAnswer aa = new AttemptAnswer();
        aa.setAttempt(attempt);
        PaperQuestion pq = new PaperQuestion();
        pq.setId(1L);
        pq.setContentSnapshot("1+1=?");
        pq.setOptionsSnapshot("[{\"key\":\"A\",\"text\":\"2\"},{\"key\":\"B\",\"text\":\"3\"}]");
        pq.setAnswerSnapshot("A");
        pq.setAnalysisSnapshot("加法");
        aa.setPaperQuestion(pq);
        aa.setQuestionType("SINGLE");
        aa.setStudentAnswer("B");
        aa.setCorrectAnswer("A");
        aa.setIsCorrect(0);
        aa.setScore(BigDecimal.ZERO);
        aa.setFullScore(new BigDecimal("2.5"));
        when(attemptAnswerRepository.findByAttempt_IdOrderByPaperQuestion_IdAsc(2L)).thenReturn(List.of(aa));

        ExamReviewDTO review = resultService.review(EXAM_ID);

        assertEquals("期中考试", review.title());
        assertEquals(1, review.questions().size());
        var q = review.questions().get(0);
        assertEquals("1+1=?", q.content());
        assertEquals(2, q.options().size());
        assertEquals("B", q.studentAnswer());
        assertEquals("A", q.correctAnswer());
        assertEquals(0, q.correctFlag());
        assertEquals("加法", q.analysis());
        assertEquals(0, review.score().compareTo(new BigDecimal("5")));
        assertEquals(0, review.fullScore().compareTo(new BigDecimal("10")));
    }

    @Test
    void wrongBookOnlyAggregatesWrongAnswers() {
        when(attemptAnswerRepository.findByAttempt_StudentIdAndAttempt_StatusAndIsCorrect(
                STUDENT_ID, AttemptStatus.SUBMITTED, 0)).thenReturn(List.of());

        assertTrue(resultService.wrongBook(null).isEmpty());
        // 验证查询条件强制 is_correct=0 且 SUBMITTED（正确题不会进入错题本）
        org.mockito.Mockito.verify(attemptAnswerRepository)
                .findByAttempt_StudentIdAndAttempt_StatusAndIsCorrect(STUDENT_ID, AttemptStatus.SUBMITTED, 0);
    }

    @Test
    void wrongBookWithCourseFilter() {
        when(attemptAnswerRepository.findByAttempt_StudentIdAndAttempt_StatusAndIsCorrectAndAttempt_Exam_Course_Id(
                STUDENT_ID, AttemptStatus.SUBMITTED, 0, 1L)).thenReturn(List.of());

        assertTrue(resultService.wrongBook(1L).isEmpty());
        org.mockito.Mockito.verify(attemptAnswerRepository)
                .findByAttempt_StudentIdAndAttempt_StatusAndIsCorrectAndAttempt_Exam_Course_Id(
                        STUDENT_ID, AttemptStatus.SUBMITTED, 0, 1L);
    }
}
