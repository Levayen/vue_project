package com.example.app.service;

import com.example.app.entity.QuestionType;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 答案匹配纯逻辑（SPEC-grading）
 * 判分为无副作用纯函数，独立于 JPA/Spring，便于高覆盖单测。
 * 规则与 question-bank 答案编码严格一致：
 * - SINGLE/JUDGE：归一化（去空白与分隔符、转大写）后完全相等得满分，否则 0
 * - MULTI：选项集合完全相等得满分；漏选/错选一律 0（无部分分）
 * - FILL：归一化（trim + 全角转半角 + 忽略大小写）后命中任一可接受答案（|| 分隔）得满分
 * 未作答（student 为 null/空白）一律 0 分。
 */
public final class AnswerMatcher {

    /**
     * 判分结果：是否正确 + 实得分
     */
    public record MatchResult(boolean correct, BigDecimal score) {}

    private AnswerMatcher() {
    }

    /**
     * 按题型比对学生答案与正确答案
     *
     * @param type    题型
     * @param student 学生答案编码（可为 null 表示未作答）
     * @param correct 正确答案快照
     * @param full    本题满分
     * @return 判分结果
     */
    public static MatchResult match(QuestionType type, String student, String correct, BigDecimal full) {
        if (student == null || student.isBlank()) {
            return new MatchResult(false, BigDecimal.ZERO);
        }
        boolean ok = switch (type) {
            case SINGLE, JUDGE -> normalizeLetter(student).equals(normalizeLetter(correct));
            case MULTI -> toLetterSet(student).equals(toLetterSet(correct));
            case FILL -> matchFill(student, correct);
            case SHORT_ANSWER, ESSAY -> throw new IllegalStateException(
                    "主观题（" + type + "）不参与自动判分，应由人工阅卷流程评分");
        };
        return new MatchResult(ok, ok ? full : BigDecimal.ZERO);
    }

    /**
     * 选择类归一化：去首尾空白、去空白与中英文逗号分隔符、转大写
     */
    static String normalizeLetter(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase().replaceAll("[\\s,，]+", "");
    }

    /**
     * 多选拆为字母集合（顺序无关）
     */
    static Set<String> toLetterSet(String raw) {
        Set<String> set = new HashSet<>();
        for (char c : normalizeLetter(raw).toCharArray()) {
            set.add(String.valueOf(c));
        }
        return set;
    }

    /**
     * 填空：归一化后与任一可接受答案（|| 分隔）相等
     */
    private static boolean matchFill(String student, String correct) {
        if (correct == null) {
            return false;
        }
        String norm = normalizeFill(student);
        for (String accept : correct.split("\\|\\|")) {
            if (normalizeFill(accept).equals(norm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 填空归一化：全角字符转半角 + trim + 忽略大小写
     */
    static String normalizeFill(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.trim().toCharArray()) {
            if (c == '\u3000') {
                c = ' ';
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                c = (char) (c - 0xFEE0);
            }
            sb.append(c);
        }
        return sb.toString().trim().toLowerCase();
    }
}
