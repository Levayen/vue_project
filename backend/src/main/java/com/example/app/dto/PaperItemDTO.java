package com.example.app.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷题目条目 DTO（SPEC-exam-paper + M7 主观题）
 * 请求侧仅用 questionId/seq/score；响应侧附快照内容（type/content/options/answer）。
 * referenceAnswer 为主观题参考答案（仅响应侧；组卷时直接从题库实体拷贝快照）。
 */
public record PaperItemDTO(
        Long questionId,
        Integer seq,
        BigDecimal score,
        String type,
        String content,
        List<QuestionOption> options,
        String answer,
        String analysis,
        String referenceAnswer
) {
}
