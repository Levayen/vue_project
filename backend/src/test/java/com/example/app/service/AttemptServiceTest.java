package com.example.app.service;

import com.example.app.dto.ExamQuestionDTO;
import com.example.app.dto.StartExamResponse;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Course;
import com.example.app.entity.Enrollment;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.SysUser;
import com.example.app.entity.UserRole;
import com.example.app.exception.BusinessException;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRepository;
import com.example.app.repository.PaperQuestionRepository;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AttemptService 单元测试（SPEC-exam-session）
 * 覆盖入场校验（选课/时间/次数）、续考恢复、开卷/闭卷答案可见性、
 * 暂存/违规上报、交卷判分、截止时间兜底。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttemptServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamAttemptRepository attemptRepository;
    @Mock
    private PaperQuestionRepository paperQuestionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private GradingPort gradingPort;

    private AttemptService attemptService;
    private Course course;
    private ExamPaper paper;
    private Exam exam;
    private PaperQuestion singleQ;
    private PaperQuestion judgeQ;

    private final LocalDateTime now = LocalDateTime.of(2026, 9, 10, 10, 0);

    @BeforeEach
    void setUp() {
        attemptService = new AttemptService(examRepository, attemptRepository, paperQuestionRepository,
                enrollmentRepository, sysUserRepository, gradingPort, new ObjectMapper());
        attemptService.setClock(Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));

        course = new Course();
        course.setId(1L);
        course.setCourseName("数据结构");

        paper = new ExamPaper();
        paper.setId(10L);
        paper.setName("数据结构卷");
        paper.setCourse(course);
        paper.setTotalScore(new BigDecimal("10"));

        exam = new Exam();
        exam.setId(1L);
        exam.setTitle("期中测试");
        exam.setPaper(paper);
        exam.setCourse(course);
        exam.setStartTime(now.minusHours(1));
        exam.setEndTime(now.plusHours(2));
        exam.setDurationMinutes(120);
        exam.setMaxAttempts(2);
        exam.setShuffle(false);
        exam.setOpenBook(false);

        singleQ = new PaperQuestion();
        singleQ.setId(100L);
        singleQ.setQuestionId(1L);
        singleQ.setTypeSnapshot("SINGLE");
        singleQ.setContentSnapshot("单选题干");
        singleQ.setOptionsSnapshot("[{\"key\":\"A\",\"text\":\"甲\"},{\"key\":\"B\",\"text\":\"乙\"}]");
        singleQ.setAnswerSnapshot("A");
        singleQ.setAnalysisSnapshot("解析A");
        singleQ.setScore(new BigDecimal("5"));

        judgeQ = new PaperQuestion();
        judgeQ.setId(101L);
        judgeQ.setQuestionId(2L);
        judgeQ.setTypeSnapshot("JUDGE");
        judgeQ.setContentSnapshot("判断题干");
        judgeQ.setAnswerSnapshot("T");
        judgeQ.setScore(new BigDecimal("5"));

        // 当前学生身份：sys_user id=1 → studentId=100
        SysUser sysUser = new SysUser();
        sysUser.setId(1L);
        sysUser.setUsername("stu1");
        sysUser.setRole(UserRole.STUDENT);
        sysUser.setStudentId(100L);
        UserContext.set(new UserContext.CurrentUser(1L, "stu1", UserRole.STUDENT));
        when(sysUserRepository.findById(1L)).thenReturn(Optional.of(sysUser));

        // 已选课
        Enrollment en = new Enrollment();
        en.setStudent(null);
        en.setCourse(course);
        when(enrollmentRepository.findByStudentIdAndCourseId(100L, 1L)).thenReturn(Optional.of(en));

        when(paperQuestionRepository.findByPaper_IdOrderBySeqAsc(10L)).thenReturn(List.of(singleQ, judgeQ));
        when(attemptRepository.countByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.SUBMITTED)).thenReturn(0L);
    }

    @AfterEach
    void clear() {
        UserContext.clear();
    }

    @Test
    void startNewAttemptReturnsQuestionsWithoutAnswerWhenClosedBook() {
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> {
            ExamAttempt a = inv.getArgument(0);
            a.setId(500L);
            return a;
        });

        StartExamResponse resp = attemptService.start(1L);

        assertEquals(2, resp.questions().size());
        // 闭卷：答案与解析均为 null
        assertNull(resp.questions().get(0).answer());
        assertNull(resp.questions().get(0).analysis());
        assertFalse(resp.openBook());
        assertTrue(resp.remainingSeconds() > 0);
        verify(attemptRepository).save(any(ExamAttempt.class));
    }

    @Test
    void startReturnsAnswerWhenOpenBook() {
        exam.setOpenBook(true);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> {
            ExamAttempt a = inv.getArgument(0);
            a.setId(500L);
            return a;
        });

        StartExamResponse resp = attemptService.start(1L);

        assertTrue(resp.openBook());
        assertEquals("A", resp.questions().get(0).answer());
        assertEquals("解析A", resp.questions().get(0).analysis());
    }

    @Test
    void startRestoresInProgressAttempt() {
        ExamAttempt existing = new ExamAttempt();
        existing.setId(500L);
        existing.setExam(exam);
        existing.setStudentId(100L);
        existing.setStatus(AttemptStatus.IN_PROGRESS);
        existing.setStartTime(now.minusMinutes(10));
        existing.setDeadline(now.plusMinutes(110));
        existing.setAnswersJson("{\"100\":\"A\"}");

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existing));

        StartExamResponse resp = attemptService.start(1L);

        assertEquals(500L, resp.attemptId());
        assertEquals("{\"100\":\"A\"}", resp.existingAnswers());
        // 续考不新建 attempt
        verify(attemptRepository, never()).save(any(ExamAttempt.class));
    }

    @Test
    void rejectNotEnrolled() {
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(enrollmentRepository.findByStudentIdAndCourseId(100L, 1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> attemptService.start(1L));
        assertTrue(ex.getMessage().contains("未选修"));
    }

    @Test
    void rejectBeforeStartTime() {
        exam.setStartTime(now.plusHours(1));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        BusinessException ex = assertThrows(BusinessException.class, () -> attemptService.start(1L));
        assertTrue(ex.getMessage().contains("尚未开始"));
    }

    @Test
    void rejectAfterEndTime() {
        exam.setEndTime(now.minusMinutes(1));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        BusinessException ex = assertThrows(BusinessException.class, () -> attemptService.start(1L));
        assertTrue(ex.getMessage().contains("已截止"));
    }

    @Test
    void rejectAttemptsUsedUp() {
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(attemptRepository.countByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.SUBMITTED))
                .thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> attemptService.start(1L));
        assertTrue(ex.getMessage().contains("次数"));
    }

    @Test
    void saveAnswersSuccess() {
        ExamAttempt attempt = inProgressAttempt();
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        attemptService.saveAnswers(1L, Map.of(100L, "A"));

        verify(attemptRepository).save(attempt);
        assertTrue(attempt.getAnswersJson().contains("100"));
        assertEquals(AttemptStatus.IN_PROGRESS, attempt.getStatus());
    }

    @Test
    void saveAnswersAfterDeadlineForcesSubmit() {
        ExamAttempt attempt = inProgressAttempt();
        attempt.setDeadline(now.minusMinutes(1)); // 已过截止
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(gradingPort.grade(any(ExamAttempt.class))).thenAnswer(inv -> {
            ExamAttempt a = inv.getArgument(0);
            a.setStatus(AttemptStatus.SUBMITTED);
            a.setScore(new BigDecimal("5"));
            return new GradingPort.GradeResult(new BigDecimal("5"), false);
        });

        BusinessException ex = assertThrows(BusinessException.class,
                () -> attemptService.saveAnswers(1L, Map.of(100L, "A")));
        assertTrue(ex.getMessage().contains("自动交卷"));
        assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
        assertEquals(new BigDecimal("5"), attempt.getScore());
    }

    @Test
    void submitComputesScore() {
        ExamAttempt attempt = inProgressAttempt();
        attempt.setAnswersJson("{\"100\":\"A\"}");
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));
        when(gradingPort.grade(any(ExamAttempt.class))).thenReturn(
                new GradingPort.GradeResult(new BigDecimal("5"), false));

        Map<String, Object> result = attemptService.submit(1L);

        assertEquals(new BigDecimal("5"), result.get("score"));
        assertEquals(new BigDecimal("10"), result.get("totalScore"));
        assertEquals(Boolean.FALSE, result.get("grading"));
        verify(gradingPort).grade(attempt);
    }

    @Test
    void reportViolationIncrementsCount() {
        ExamAttempt attempt = inProgressAttempt();
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(attempt));

        attemptService.reportViolation(1L, "VISIBILITY");
        attemptService.reportViolation(1L, "COPY");

        assertEquals(2, attempt.getViolationCount());
        assertNotNull(attempt.getViolationsJson());
        verify(attemptRepository, times(2)).save(attempt);
    }

    @Test
    void deadlineIsMinOfDurationAndExamEnd() {
        // duration=120min but end_time is only 30min away → deadline = end_time
        exam.setEndTime(now.plusMinutes(30));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        StartExamResponse resp = attemptService.start(1L);

        // 剩余时间应约为 30 分钟（1800 秒），不是 120 分钟
        assertTrue(resp.remainingSeconds() <= 1800 && resp.remainingSeconds() > 1700);
    }

    private ExamAttempt inProgressAttempt() {
        ExamAttempt a = new ExamAttempt();
        a.setId(500L);
        a.setExam(exam);
        a.setStudentId(100L);
        a.setStatus(AttemptStatus.IN_PROGRESS);
        a.setStartTime(now.minusMinutes(10));
        a.setDeadline(now.plusMinutes(110));
        a.setViolationCount(0);
        return a;
    }
}
