package com.example.app.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学生答题页题目 DTO（SPEC-exam-session）
 * 闭卷考试 answer 为 null（脱敏）；开卷考试 answer 含正确答案。
 */
public record ExamQuestionDTO(
        Long paperQuestionId,
        String type,
        String content,
        List<QuestionOption> options,
        BigDecimal score,
        String answer,
        String analysis
) {
}
