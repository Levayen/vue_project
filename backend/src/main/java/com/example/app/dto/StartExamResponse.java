package com.example.app.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开考响应 DTO（SPEC-exam-session）
 * 含本次尝试 id、截止时刻、剩余秒数、题目列表（按开卷/闭卷决定是否含答案）。
 */
public record StartExamResponse(
        Long attemptId,
        Long examId,
        String title,
        Boolean openBook,
        LocalDateTime deadline,
        Long remainingSeconds,
        List<ExamQuestionDTO> questions,
        String existingAnswers
) {
}
