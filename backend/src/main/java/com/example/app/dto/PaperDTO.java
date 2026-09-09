package com.example.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷 DTO（SPEC-exam-paper）
 * 请求：手动组卷仅需 name/courseId/items(questionId, seq, score)；
 * 响应：列表不含 items，详情含快照题目（教师视图带答案）。
 */
public record PaperDTO(
        Long id,
        String name,
        Long courseId,
        String courseName,
        BigDecimal totalScore,
        String generateType,
        String status,
        List<PaperItemDTO> items,
        LocalDateTime createTime
) {
}
