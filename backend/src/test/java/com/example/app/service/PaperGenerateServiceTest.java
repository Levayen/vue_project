package com.example.app.service;

import com.example.app.dto.DrawRule;
import com.example.app.entity.Question;
import com.example.app.entity.QuestionType;
import com.example.app.exception.BusinessException;
import com.example.app.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * PaperGenerateService 单元测试（SPEC-exam-paper）
 * 覆盖抽题成功/题量不足/难度过滤/跨难度抽取/不重复/参数校验分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaperGenerateServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    private PaperGenerateService generateService;

    @BeforeEach
    void setUp() {
        generateService = new PaperGenerateService(questionRepository);
    }

    private Question question(long id, QuestionType type, int difficulty) {
        Question q = new Question();
        q.setId(id);
        q.setType(type);
        q.setDifficulty(difficulty);
        q.setContent("题目" + id);
        q.setAnswer("A");
        return q;
    }

    private void mockPool(QuestionType type, List<Question> pool) {
        when(questionRepository.findByCourse_IdAndTypeOrderByIdAsc(1L, type)).thenReturn(pool);
    }

    private List<Question> singlePool(int size, int difficulty) {
        List<Question> pool = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            pool.add(question(i, QuestionType.SINGLE, difficulty));
        }
        return pool;
    }

    private DrawRule rule(String type, Integer difficulty, int count, String score) {
        return new DrawRule(type, difficulty, count, new BigDecimal(score));
    }

    // ---------- 抽题成功 ----------

    @Test
    void drawSuccessReturnsExactCountWithoutDuplicates() {
        mockPool(QuestionType.SINGLE, singlePool(5, 1));

        List<Question> drawn = generateService.draw(1L, rule("SINGLE", null, 3, "2"));

        assertEquals(3, drawn.size());
        Set<Long> ids = new HashSet<>();
        drawn.forEach(q -> assertTrue(ids.add(q.getId()), "抽中题目不应重复"));
    }

    @Test
    void drawDifficultyFilterOnlyPicksMatchingDifficulty() {
        // 池中 3 道难度2 + 5 道难度1，只抽难度2 → 只可能来自难度2子集
        List<Question> pool = new ArrayList<>();
        pool.addAll(singlePool(3, 2));
        pool.addAll(singlePool(5, 1));
        mockPool(QuestionType.SINGLE, pool);

        List<Question> drawn = generateService.draw(1L, rule("SINGLE", 2, 3, "2"));

        assertEquals(3, drawn.size());
        assertTrue(drawn.stream().allMatch(q -> q.getDifficulty() == 2));
    }

    @Test
    void drawNullDifficultyCrossesAllDifficulties() {
        List<Question> pool = new ArrayList<>();
        pool.addAll(singlePool(2, 1));
        pool.addAll(singlePool(2, 2));
        pool.addAll(singlePool(2, 3));
        mockPool(QuestionType.SINGLE, pool);

        List<Question> drawn = generateService.draw(1L, rule("SINGLE", null, 6, "2"));

        assertEquals(6, drawn.size());
        assertTrue(drawn.stream().anyMatch(q -> q.getDifficulty() == 1));
        assertTrue(drawn.stream().anyMatch(q -> q.getDifficulty() == 3));
    }

    @Test
    void drawJudgementTypePool() {
        List<Question> pool = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            pool.add(question(i, QuestionType.JUDGE, 1));
        }
        mockPool(QuestionType.JUDGE, pool);

        List<Question> drawn = generateService.draw(1L, rule("JUDGE", null, 2, "2"));

        assertEquals(2, drawn.size());
        assertTrue(drawn.stream().allMatch(q -> q.getType() == QuestionType.JUDGE));
    }

    // ---------- 题量不足 ----------

    @Test
    void drawInsufficientThrowsWithGapMessage() {
        mockPool(QuestionType.SINGLE, singlePool(2, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> generateService.draw(1L, rule("SINGLE", null, 5, "2")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("5"));
        assertTrue(ex.getMessage().contains("2"));
    }

    @Test
    void drawInsufficientWithDifficultyFilterReportsGap() {
        mockPool(QuestionType.MULTI, singlePool(3, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> generateService.draw(1L, rule("MULTI", 3, 2, "4")));

        assertTrue(ex.getMessage().contains("难度3"));
    }

    // ---------- 参数校验 ----------

    @Test
    void drawRejectsZeroCount() {
        assertThrows(BusinessException.class, () -> generateService.draw(1L, rule("SINGLE", null, 0, "2")));
    }

    @Test
    void drawRejectsUnknownType() {
        assertThrows(BusinessException.class, () -> generateService.draw(1L, rule("ESSAY", null, 1, "2")));
    }

    @Test
    void scorePerQuestionValidation() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> generateService.validateScorePerQuestion(BigDecimal.ZERO));
        assertTrue(ex.getMessage().contains("分值"));

        assertThrows(BusinessException.class,
                () -> generateService.validateScorePerQuestion(new BigDecimal("101")));

        assertDoesNotThrow(() -> generateService.validateScorePerQuestion(new BigDecimal("2.5")));
    }
}
