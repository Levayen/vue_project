package com.example.app.service;

import com.example.app.dto.ReviewAttemptDTO;
import com.example.app.dto.ScoreRequest;
import com.example.app.entity.AttemptAnswer;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Course;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.Student;
import com.example.app.exception.BusinessException;
import com.example.app.repository.AttemptAnswerRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReviewService 单元测试（M7 人工阅卷）
 * 覆盖：评分边界（空值/超满分/非主观题拒绝）、满分记正确/部分分记 0、评语保存、
 * 全部评完自动定稿、答卷详情主观题过滤、待阅列表聚合。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    @Mock
    private ExamAttemptRepository attemptRepository;
    @Mock
    private AttemptAnswerRepository answerRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GradingPort gradingPort;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(attemptRepository, answerRepository,
                studentRepository, gradingPort);
    }

    private AttemptAnswer subjectiveAnswer(long answerId, String type, String fullScore) {
        PaperQuestion pq = new PaperQuestion();
        pq.setId(900L);
        pq.setTypeSnapshot(type);
        pq.setContentSnapshot("请简述 MVC 架构");
        pq.setReferenceAnswerSnapshot("Model-View-Controller 三层分离");

        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(1L);
        attempt.setStatus(AttemptStatus.PENDING_REVIEW);
        Exam exam = new Exam();
        exam.setId(10L);
        attempt.setExam(exam);

        AttemptAnswer aa = new AttemptAnswer();
        aa.setId(answerId);
        aa.setAttempt(attempt);
        aa.setPaperQuestion(pq);
        aa.setQuestionType(type);
        aa.setFullScore(new BigDecimal(fullScore));
        aa.setIsCorrect(3);
        aa.setScore(BigDecimal.ZERO);
        aa.setAnswerText("学生的作答内容");
        return aa;
    }

    @Test
    void scoreAnswerRejectsNullScore() {
        AttemptAnswer aa = subjectiveAnswer(50L, "SHORT_ANSWER", "5");
        when(answerRepository.findById(50L)).thenReturn(Optional.of(aa));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewService.scoreAnswer(50L, new ScoreRequest(null, null)));
        assertTrue(ex.getMessage().contains("得分"));
        verify(gradingPort, never()).finalizeAttempt(any());
    }

    @Test
    void scoreAnswerRejectsOverMaxScore() {
        AttemptAnswer aa = subjectiveAnswer(50L, "SHORT_ANSWER", "5");
        when(answerRepository.findById(50L)).thenReturn(Optional.of(aa));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewService.scoreAnswer(50L, new ScoreRequest(new BigDecimal("6"), null)));
        assertTrue(ex.getMessage().contains("0"));
        verify(answerRepository, never()).save(any());
    }

    @Test
    void scoreAnswerRejectsNegativeScore() {
        AttemptAnswer aa = subjectiveAnswer(50L, "SHORT_ANSWER", "5");
        when(answerRepository.findById(50L)).thenReturn(Optional.of(aa));

        assertThrows(BusinessException.class,
                () -> reviewService.scoreAnswer(50L, new ScoreRequest(new BigDecimal("-1"), null)));
    }

    @Test
    void scoreAnswerRejectsObjectiveQuestion() {
        AttemptAnswer aa = subjectiveAnswer(50L, "SINGLE", "5");
        when(answerRepository.findById(50L)).thenReturn(Optional.of(aa));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewService.scoreAnswer(50L, new ScoreRequest(new BigDecimal("5"), null)));
        assertTrue(ex.getMessage().contains("主观题"));
    }

    @Test
    void scoreAnswerNotFound() {
        when(answerRepository.findById(404L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewService.scoreAnswer(404L, new ScoreRequest(new BigDecimal("1"), null)));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void fullMarksMarksCorrectAndNoFinalizeWhileOthersPending() {
        AttemptAnswer aa = subjectiveAnswer(50L, "SHORT_ANSWER", "5");
        when(answerRepository.findById(50L)).thenReturn(Optional.of(aa));
        when(answerRepository.countByAttempt_IdAndIsCorrect(1L, 3)).thenReturn(1L);

        Map<String, Object> resp = reviewService.scoreAnswer(
                50L, new ScoreRequest(new BigDecimal("5"), "回答完整"));

        assertEquals(1, aa.getIsCorrect());
        assertEquals(0, aa.getScore().compareTo(new BigDecimal("5")));
        assertEquals("回答完整", aa.getTeacherComment());
        assertEquals(1L, resp.get("remaining"));
        assertEquals(Boolean.FALSE, resp.get("finalized"));
        verify(answerRepository).save(aa);
        verify(gradingPort, never()).finalizeAttempt(any());
    }

    @Test
    void partialMarksMarksIncorrectAndBlankCommentBecomesNull() {
        AttemptAnswer aa = subjectiveAnswer(50L, "ESSAY", "10");
        when(answerRepository.findById(50L)).thenReturn(Optional.of(aa));
        when(answerRepository.countByAttempt_IdAndIsCorrect(1L, 3)).thenReturn(0L);

        Map<String, Object> resp = reviewService.scoreAnswer(
                50L, new ScoreRequest(new BigDecimal("7.5"), "  "));

        // 部分分记 0（错题本可收录口径）
        assertEquals(0, aa.getIsCorrect());
        assertEquals(0, aa.getScore().compareTo(new BigDecimal("7.5")));
        assertNull(aa.getTeacherComment());
        assertEquals(0L, resp.get("remaining"));
        assertEquals(Boolean.TRUE, resp.get("finalized"));
        // 最后一题评完 → 自动定稿
        verify(gradingPort).finalizeAttempt(aa.getAttempt());
    }

    @Test
    void zeroScoreAllowed() {
        AttemptAnswer aa = subjectiveAnswer(50L, "SHORT_ANSWER", "5");
        when(answerRepository.findById(50L)).thenReturn(Optional.of(aa));
        when(answerRepository.countByAttempt_IdAndIsCorrect(1L, 3)).thenReturn(0L);

        reviewService.scoreAnswer(50L, new ScoreRequest(BigDecimal.ZERO, "跑题"));

        assertEquals(0, aa.getIsCorrect());
        assertEquals(BigDecimal.ZERO, aa.getScore());
        assertEquals("跑题", aa.getTeacherComment());
        verify(gradingPort).finalizeAttempt(any());
    }

    @Test
    void reviewAttemptListsOnlySubjectiveQuestions() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(1L);
        attempt.setStatus(AttemptStatus.PENDING_REVIEW);
        attempt.setScore(new BigDecimal("2.5"));
        ExamPaper paper = new ExamPaper();
        paper.setTotalScore(new BigDecimal("15"));
        Exam exam = new Exam();
        exam.setId(10L);
        exam.setTitle("期末考试");
        exam.setPaper(paper);
        attempt.setExam(exam);

        Student student = new Student();
        student.setId(5L);
        student.setName("张三");
        student.setStudentNumber("S001");
        when(studentRepository.findById(5L)).thenReturn(Optional.of(student));
        attempt.setStudentId(5L);

        AttemptAnswer objective = new AttemptAnswer();
        objective.setQuestionType("SINGLE");
        PaperQuestion opq = new PaperQuestion();
        opq.setTypeSnapshot("SINGLE");
        objective.setPaperQuestion(opq);

        AttemptAnswer subjective = subjectiveAnswer(50L, "SHORT_ANSWER", "5");
        subjective.setAttempt(attempt);

        when(attemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(answerRepository.findByAttempt_IdOrderByPaperQuestion_IdAsc(1L))
                .thenReturn(List.of(objective, subjective));

        ReviewAttemptDTO dto = reviewService.reviewAttempt(1L);

        assertEquals(1, dto.answers().size());
        ReviewAttemptDTO.SubjectiveAnswerDTO sa = dto.answers().get(0);
        assertEquals(50L, sa.answerId());
        assertEquals("请简述 MVC 架构", sa.content());
        assertEquals("Model-View-Controller 三层分离", sa.referenceAnswer());
        assertEquals("学生的作答内容", sa.answerText());
        assertEquals(3, sa.correctFlag());
        assertNull(sa.score()); // 待评未给分
        assertFalse(dto.finalized());
        assertEquals(0, dto.currentScore().compareTo(new BigDecimal("2.5")));
        assertEquals(0, dto.paperTotalScore().compareTo(new BigDecimal("15")));
    }

    @Test
    void reviewAttemptRejectsInProgress() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(1L);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        when(attemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewService.reviewAttempt(1L));
        assertTrue(ex.getMessage().contains("尚未交卷"));
    }

    @Test
    void pendingExamsAggregatesByExam() {
        Course course = new Course();
        course.setCourseName("软件工程");
        Exam exam = new Exam();
        exam.setId(10L);
        exam.setTitle("期末");
        exam.setCourse(course);

        ExamAttempt a1 = new ExamAttempt();
        a1.setId(1L);
        a1.setExam(exam);
        a1.setStudentId(5L);
        ExamAttempt a2 = new ExamAttempt();
        a2.setId(2L);
        a2.setExam(exam);
        a2.setStudentId(6L);

        when(attemptRepository.findByStatus(AttemptStatus.PENDING_REVIEW))
                .thenReturn(List.of(a1, a2));
        when(answerRepository.countByAttempt_IdAndIsCorrect(1L, 3)).thenReturn(2L);
        when(answerRepository.countByAttempt_IdAndIsCorrect(2L, 3)).thenReturn(1L);

        var list = reviewService.pendingExams();

        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).examId());
        assertEquals(2L, list.get(0).pendingAttempts());
        assertEquals(3L, list.get(0).pendingQuestions());
        assertEquals("软件工程", list.get(0).courseName());
    }
}
