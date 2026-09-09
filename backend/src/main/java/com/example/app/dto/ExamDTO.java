package com.example.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试 DTO（SPEC-exam-session）
 * 请求：发布考试；响应：考试列表/详情。
 */
public record ExamDTO(
        Long id,
        Long paperId,
        String paperName,
        Long courseId,
        String courseName,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        Integer maxAttempts,
        String scoreRule,
        Boolean shuffle,
        Boolean openBook,
        BigDecimal totalScore
) {
}
