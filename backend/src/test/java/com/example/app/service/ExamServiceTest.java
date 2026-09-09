package com.example.app.service;

import com.example.app.dto.ExamDTO;
import com.example.app.entity.AttemptStatus;
import com.example.app.entity.Course;
import com.example.app.entity.Enrollment;
import com.example.app.entity.Exam;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.PaperStatus;
import com.example.app.entity.Student;
import com.example.app.exception.BusinessException;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamPaperRepository;
import com.example.app.repository.ExamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ExamService 单元测试（SPEC-exam-session）
 * 覆盖发布考试校验（试卷状态、时间窗口、时长、次数）与监考视图。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamPaperRepository paperRepository;
    @Mock
    private ExamAttemptRepository attemptRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private GradingPort gradingPort;

    private ExamService examService;
    private Course course;
    private ExamPaper publishedPaper;
    private ExamPaper draftPaper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        examService = new ExamService(examRepository, paperRepository, attemptRepository,
                enrollmentRepository, gradingPort, objectMapper);
        course = new Course();
        course.setId(1L);
        course.setCourseName("数据结构");

        publishedPaper = new ExamPaper();
        publishedPaper.setId(10L);
        publishedPaper.setName("数据结构卷");
        publishedPaper.setCourse(course);
        publishedPaper.setStatus(PaperStatus.PUBLISHED);
        publishedPaper.setTotalScore(new BigDecimal("100"));

        draftPaper = new ExamPaper();
        draftPaper.setId(11L);
        draftPaper.setName("草稿卷");
        draftPaper.setCourse(course);
        draftPaper.setStatus(PaperStatus.DRAFT);
    }

    private ExamDTO basePublish(Long paperId) {
        return new ExamDTO(null, paperId, null, null, null, "期中测试",
                LocalDateTime.of(2026, 9, 10, 9, 0),
                LocalDateTime.of(2026, 9, 10, 11, 0),
                120, 1, "LAST", true, false, null);
    }

    @Test
    void publishSuccess() {
        when(paperRepository.findById(10L)).thenReturn(Optional.of(publishedPaper));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        ExamDTO result = examService.publish(basePublish(10L));

        assertEquals("期中测试", result.title());
        assertFalse(result.openBook());
        assertNotNull(result.id());
        assertEquals(new BigDecimal("100"), result.totalScore());
        verify(examRepository).save(any(Exam.class));
    }

    @Test
    void rejectDraftPaper() {
        when(paperRepository.findById(11L)).thenReturn(Optional.of(draftPaper));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> examService.publish(basePublish(11L)));
        assertTrue(ex.getMessage().contains("已发布"));
    }

    @Test
    void rejectEndNotAfterStart() {
        when(paperRepository.findById(10L)).thenReturn(Optional.of(publishedPaper));
        ExamDTO dto = new ExamDTO(null, 10L, null, null, null, "x",
                LocalDateTime.of(2026, 9, 10, 11, 0),
                LocalDateTime.of(2026, 9, 10, 9, 0),
                120, 1, "LAST", true, false, null);
        assertThrows(BusinessException.class, () -> examService.publish(dto));
    }

    @Test
    void rejectNonPositiveDuration() {
        when(paperRepository.findById(10L)).thenReturn(Optional.of(publishedPaper));
        ExamDTO dto = new ExamDTO(null, 10L, null, null, null, "x",
                LocalDateTime.of(2026, 9, 10, 9, 0),
                LocalDateTime.of(2026, 9, 10, 11, 0),
                0, 1, "LAST", true, false, null);
        assertThrows(BusinessException.class, () -> examService.publish(dto));
    }

    @Test
    void rejectNonPositiveAttempts() {
        when(paperRepository.findById(10L)).thenReturn(Optional.of(publishedPaper));
        ExamDTO dto = new ExamDTO(null, 10L, null, null, null, "x",
                LocalDateTime.of(2026, 9, 10, 9, 0),
                LocalDateTime.of(2026, 9, 10, 11, 0),
                120, 0, "LAST", true, false, null);
        assertThrows(BusinessException.class, () -> examService.publish(dto));
    }

    @Test
    void publishDefaults() {
        when(paperRepository.findById(10L)).thenReturn(Optional.of(publishedPaper));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        // scoreRule=null, shuffle=null, openBook=null → defaults LAST, true, false
        ExamDTO dto = new ExamDTO(null, 10L, null, null, null, "x",
                LocalDateTime.of(2026, 9, 10, 9, 0),
                LocalDateTime.of(2026, 9, 10, 11, 0),
                120, 1, null, null, null, null);
        ExamDTO result = examService.publish(dto);
        assertEquals("LAST", result.scoreRule());
        assertTrue(result.shuffle());
        assertFalse(result.openBook());
    }

    @Test
    void monitoringReturnsEnrolledStudents() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setTitle("期中测试");
        exam.setCourse(course);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        Student s1 = new Student();
        s1.setId(100L);
        s1.setName("张三");
        s1.setStudentNumber("2024001");
        Enrollment en1 = new Enrollment();
        en1.setStudent(s1);
        when(enrollmentRepository.findByCourseId(1L)).thenReturn(List.of(en1));
        when(attemptRepository.findByExam_IdOrderByStartTimeDesc(1L)).thenReturn(List.of());

        var result = examService.monitoring(1L);
        assertEquals(1, result.students().size());
        assertEquals("张三", result.students().get(0).studentName());
        assertEquals("未开始", result.students().get(0).status());
        // 汇总统计：1 人选课，0 进行中，0 已交卷，1 未开始
        assertEquals(1, result.enrolledCount());
        assertEquals(0, result.inProgressCount());
        assertEquals(0, result.submittedCount());
        assertEquals(1, result.notStartedCount());
        assertEquals(0, result.totalViolations());
    }

    @Test
    void monitoringCountsInProgressAndSubmitted() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setCourse(course);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        Student s1 = new Student();
        s1.setId(100L);
        s1.setName("张三");
        s1.setStudentNumber("2024001");
        Student s2 = new Student();
        s2.setId(101L);
        s2.setName("李四");
        s2.setStudentNumber("2024002");
        Enrollment en1 = new Enrollment();
        en1.setStudent(s1);
        Enrollment en2 = new Enrollment();
        en2.setStudent(s2);
        when(enrollmentRepository.findByCourseId(1L)).thenReturn(List.of(en1, en2));

        // s1 进行中，s2 已交卷
        ExamAttempt a1 = new ExamAttempt();
        a1.setStudentId(100L);
        a1.setStatus(AttemptStatus.IN_PROGRESS);
        a1.setStartTime(LocalDateTime.now().minusMinutes(10));
        a1.setDeadline(LocalDateTime.now().plusMinutes(50));
        a1.setViolationCount(2);
        ExamAttempt a2 = new ExamAttempt();
        a2.setStudentId(101L);
        a2.setStatus(AttemptStatus.SUBMITTED);
        a2.setScore(new BigDecimal("80"));
        a2.setViolationCount(0);
        when(attemptRepository.findByExam_IdOrderByStartTimeDesc(1L)).thenReturn(List.of(a1, a2));

        var result = examService.monitoring(1L);
        assertEquals(2, result.enrolledCount());
        assertEquals(1, result.inProgressCount());
        assertEquals(1, result.submittedCount());
        assertEquals(0, result.notStartedCount());
        assertEquals(2, result.totalViolations());
        // 进行中学生有剩余秒数
        var s1Info = result.students().stream().filter(s -> s.studentId().equals(100L)).findFirst().orElseThrow();
        assertNotNull(s1Info.remainingSeconds());
        assertTrue(s1Info.remainingSeconds() > 0);
    }

    @Test
    void forceSubmitSingleStudent() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setCourse(course);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        ExamAttempt a1 = new ExamAttempt();
        a1.setId(10L);
        a1.setStatus(AttemptStatus.IN_PROGRESS);
        a1.setDeadline(LocalDateTime.now().plusMinutes(30));
        when(attemptRepository.findByExam_IdAndStudentIdAndStatus(1L, 100L, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(a1));
        when(gradingPort.grade(any(ExamAttempt.class))).thenAnswer(inv -> {
            ExamAttempt a = inv.getArgument(0);
            a.setStatus(AttemptStatus.SUBMITTED);
            return new GradingPort.GradeResult(new BigDecimal("75"), false);
        });
        when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = examService.forceSubmit(1L, 100L);
        assertEquals(1, count);
        assertEquals(AttemptStatus.SUBMITTED, a1.getStatus());
        verify(gradingPort).grade(a1);
    }

    @Test
    void forceSubmitAllInProgress() {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setCourse(course);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        ExamAttempt a1 = new ExamAttempt();
        a1.setStatus(AttemptStatus.IN_PROGRESS);
        a1.setDeadline(LocalDateTime.now().plusMinutes(30));
        ExamAttempt a2 = new ExamAttempt();
        a2.setStatus(AttemptStatus.SUBMITTED); // 已交卷不处理
        ExamAttempt a3 = new ExamAttempt();
        a3.setStatus(AttemptStatus.IN_PROGRESS);
        a3.setDeadline(LocalDateTime.now().plusMinutes(30));
        when(attemptRepository.findByExam_IdOrderByStartTimeDesc(1L)).thenReturn(List.of(a1, a2, a3));
        when(gradingPort.grade(any(ExamAttempt.class))).thenReturn(
                new GradingPort.GradeResult(BigDecimal.ZERO, false));
        when(attemptRepository.save(any(ExamAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = examService.forceSubmit(1L, null);
        assertEquals(2, count); // 仅 2 个 IN_PROGRESS
        verify(gradingPort, times(2)).grade(any(ExamAttempt.class));
    }
}
