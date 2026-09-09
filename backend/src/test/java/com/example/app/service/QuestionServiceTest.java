package com.example.app.service;

import com.example.app.dto.QuestionDTO;
import com.example.app.dto.QuestionOption;
import com.example.app.entity.Course;
import com.example.app.entity.Question;
import com.example.app.exception.BusinessException;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QuestionService 单元测试（SPEC-question-bank）
 * 覆盖四种题型合法创建、非法规则拒绝、答案规范化、试卷引用保护。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private QuestionReferencePort referencePort;

    private QuestionService questionService;
    private Course course;

    private List<QuestionOption> twoOptions() {
        return List.of(new QuestionOption("A", "选项A"), new QuestionOption("B", "选项B"));
    }

    private List<QuestionOption> fourOptions() {
        return List.of(
                new QuestionOption("A", "选项A"), new QuestionOption("B", "选项B"),
                new QuestionOption("C", "选项C"), new QuestionOption("D", "选项D"));
    }

    private QuestionDTO dto(String type, List<QuestionOption> options, String answer, BigDecimal score) {
        return new QuestionDTO(null, 1L, null, type, "测试题干", options, answer, score, 1, "解析");
    }

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(questionRepository, courseRepository, referencePort, new ObjectMapper());
        course = new Course();
        course.setId(1L);
        course.setCourseName("高等数学");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });
    }

    // ---------- 合法创建 ----------

    @Test
    void createSingleChoiceValid() {
        QuestionDTO result = questionService.create(dto("SINGLE", twoOptions(), "A", new BigDecimal("5")));
        assertEquals("A", result.answer());
        assertEquals(2, result.options().size());
        assertEquals("高等数学", result.courseName());
    }

    @Test
    void createMultipleChoiceNormalizesAnswer() {
        // 乱序、含逗号、小写，应规范化为排序去重的 ABD
        QuestionDTO result = questionService.create(dto("MULTI", fourOptions(), "b,D,a", new BigDecimal("10")));
        assertEquals("ABD", result.answer());
    }

    @Test
    void createJudgeNormalizesToT() {
        QuestionDTO result = questionService.create(dto("JUDGE", null, "t", new BigDecimal("5")));
        assertEquals("T", result.answer());
        assertTrue(result.options().isEmpty(), "判断题不应返回选项");
    }

    @Test
    void createFillNormalizesAnswers() {
        QuestionDTO result = questionService.create(dto("FILL", null, " 北京 || 北京市 || ", new BigDecimal("5")));
        assertEquals("北京||北京市", result.answer());
    }

    // ---------- 非法规则 ----------

    @Test
    void rejectSingleAnswerNotInOptions() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionService.create(dto("SINGLE", twoOptions(), "C", new BigDecimal("5"))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void rejectMultipleWithOnlyOneAnswer() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionService.create(dto("MULTI", fourOptions(), "A", new BigDecimal("5"))));
        assertTrue(ex.getMessage().contains("至少"));
    }

    @Test
    void rejectFillWithEmptyAnswer() {
        assertThrows(BusinessException.class,
                () -> questionService.create(dto("FILL", null, "   ", new BigDecimal("5"))));
    }

    @Test
    void rejectScoreZero() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionService.create(dto("SINGLE", twoOptions(), "A", BigDecimal.ZERO)));
        assertTrue(ex.getMessage().contains("分值"));
    }

    @Test
    void rejectScoreOver100() {
        assertThrows(BusinessException.class,
                () -> questionService.create(dto("SINGLE", twoOptions(), "A", new BigDecimal("101"))));
    }

    @Test
    void rejectChoiceWithLessThanTwoOptions() {
        List<QuestionOption> one = List.of(new QuestionOption("A", "只有一个"));
        assertThrows(BusinessException.class,
                () -> questionService.create(dto("SINGLE", one, "A", new BigDecimal("5"))));
    }

    @Test
    void rejectJudgeWithWrongAnswer() {
        assertThrows(BusinessException.class,
                () -> questionService.create(dto("JUDGE", null, "X", new BigDecimal("5"))));
    }

    @Test
    void rejectUnknownType() {
        assertThrows(BusinessException.class,
                () -> questionService.create(dto("UNKNOWN_TYPE", null, "x", new BigDecimal("5"))));
    }

    // ---------- 主观题（M7：简答/论述） ----------

    @Test
    void createShortAnswerStoresReferenceAnswer() {
        QuestionDTO result = questionService.create(
                dto("SHORT_ANSWER", null, "  答出 MVC 三层即可得分  ", new BigDecimal("8")));
        // 出参 answer 字段承载参考答案；无选项
        assertEquals("答出 MVC 三层即可得分", result.answer());
        assertTrue(result.options().isEmpty());
    }

    @Test
    void createEssayAllowsBlankReferenceAnswer() {
        QuestionDTO result = questionService.create(
                dto("ESSAY", twoOptions(), "   ", new BigDecimal("20")));
        // 主观题选项被忽略，参考答案可空，标准答案为空串
        assertNull(result.answer());
        assertTrue(result.options().isEmpty());
    }

    @Test
    void rejectDuplicateOptionKey() {
        List<QuestionOption> dup = List.of(
                new QuestionOption("A", "甲"), new QuestionOption("a", "乙"));
        assertThrows(BusinessException.class,
                () -> questionService.create(dto("SINGLE", dup, "A", new BigDecimal("5"))));
    }

    // ---------- 删除引用保护 ----------

    @Test
    void deleteReferencedQuestionConflict() {
        Question existing = new Question();
        existing.setId(9L);
        when(questionRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(referencePort.isReferencedByPaper(9L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> questionService.delete(9L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(questionRepository, never()).deleteById(any());
    }

    @Test
    void deleteUnreferencedQuestionOk() {
        Question existing = new Question();
        existing.setId(9L);
        when(questionRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(referencePort.isReferencedByPaper(9L)).thenReturn(false);

        questionService.delete(9L);
        verify(questionRepository).deleteById(9L);
    }
}
