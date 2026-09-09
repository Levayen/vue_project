package com.example.app.service;

import com.example.app.entity.QuestionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AnswerMatcher 纯判分逻辑单测（SPEC-grading 测试重点）
 * 覆盖四题型对/错/未答边界：多选全对/漏选/错选/乱序，填空归一化（空格/大小写/全角/多答案）。
 */
class AnswerMatcherTest {

    private static final BigDecimal FULL = new BigDecimal("2.5");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ---------- 单选 ----------

    @Test
    void singleCorrect() {
        var r = AnswerMatcher.match(QuestionType.SINGLE, "A", "A", FULL);
        assertTrue(r.correct());
        assertEquals(FULL, r.score());
    }

    @Test
    void singleWrong() {
        var r = AnswerMatcher.match(QuestionType.SINGLE, "B", "A", FULL);
        assertFalse(r.correct());
        assertEquals(ZERO, r.score());
    }

    @Test
    void singleLowerCaseAndSpacesNormalized() {
        var r = AnswerMatcher.match(QuestionType.SINGLE, " a ", "A", FULL);
        assertTrue(r.correct());
    }

    @Test
    void unansweredIsZero() {
        var r = AnswerMatcher.match(QuestionType.SINGLE, null, "A", FULL);
        assertFalse(r.correct());
        assertEquals(ZERO, r.score());
    }

    @Test
    void blankAnswerIsZero() {
        var r = AnswerMatcher.match(QuestionType.SINGLE, "  ", "A", FULL);
        assertFalse(r.correct());
        assertEquals(ZERO, r.score());
    }

    // ---------- 判断 ----------

    @Test
    void judgeTrueCorrect() {
        assertTrue(AnswerMatcher.match(QuestionType.JUDGE, "T", "T", FULL).correct());
    }

    @Test
    void judgeFalseCorrect() {
        assertTrue(AnswerMatcher.match(QuestionType.JUDGE, "F", "F", FULL).correct());
    }

    @Test
    void judgeMismatch() {
        assertFalse(AnswerMatcher.match(QuestionType.JUDGE, "T", "F", FULL).correct());
    }

    // ---------- 多选（无部分分） ----------

    @Test
    void multiAllCorrect() {
        assertTrue(AnswerMatcher.match(QuestionType.MULTI, "ABD", "ABD", FULL).correct());
        assertEquals(FULL, AnswerMatcher.match(QuestionType.MULTI, "ABD", "ABD", FULL).score());
    }

    @Test
    void multiOrderIrrelevant() {
        assertTrue(AnswerMatcher.match(QuestionType.MULTI, "DAB", "ABD", FULL).correct());
    }

    @Test
    void multiOneWrongChoiceIsZero() {
        var r = AnswerMatcher.match(QuestionType.MULTI, "ABC", "ABD", FULL);
        assertFalse(r.correct());
        assertEquals(ZERO, r.score());
    }

    @Test
    void multiOneMissingIsZero() {
        assertFalse(AnswerMatcher.match(QuestionType.MULTI, "AB", "ABD", FULL).correct());
    }

    @Test
    void multiMultipleMissingIsZero() {
        assertFalse(AnswerMatcher.match(QuestionType.MULTI, "A", "ABD", FULL).correct());
    }

    @Test
    void multiUnansweredIsZero() {
        assertFalse(AnswerMatcher.match(QuestionType.MULTI, null, "ABD", FULL).correct());
    }

    // ---------- 填空 ----------

    @Test
    void fillExactMatch() {
        assertTrue(AnswerMatcher.match(QuestionType.FILL, "北京", "北京", FULL).correct());
    }

    @Test
    void fillTrimAndCaseNormalized() {
        assertTrue(AnswerMatcher.match(QuestionType.FILL, "  BeIJing  ", "beijing", FULL).correct());
    }

    @Test
    void fillFullWidthNormalized() {
        // 全角字母ＡＢＣ 与数字１２３ 归一化后应等价于半角
        assertTrue(AnswerMatcher.match(QuestionType.FILL, "ＡＢＣ１２３", "abc123", FULL).correct());
    }

    @Test
    void fillFullWidthSpaceNormalized() {
        assertTrue(AnswerMatcher.match(QuestionType.FILL, "　abc　", "abc", FULL).correct());
    }

    @Test
    void fillAnyAcceptedAnswer() {
        assertTrue(AnswerMatcher.match(QuestionType.FILL, "北京市", "北京||北京市", FULL).correct());
        assertTrue(AnswerMatcher.match(QuestionType.FILL, "北京", "北京||北京市", FULL).correct());
    }

    @Test
    void fillNoMatchIsZero() {
        var r = AnswerMatcher.match(QuestionType.FILL, "上海", "北京||北京市", FULL);
        assertFalse(r.correct());
        assertEquals(ZERO, r.score());
    }

    @Test
    void fillUnansweredIsZero() {
        assertFalse(AnswerMatcher.match(QuestionType.FILL, null, "北京", FULL).correct());
    }
}
