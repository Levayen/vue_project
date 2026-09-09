package com.example.app.dto;

import java.util.List;

/**
 * 随机组卷请求 DTO（SPEC-exam-paper）
 */
public record RandomPaperRuleDTO(
        String name,
        Long courseId,
        List<DrawRule> rules
) {
}
