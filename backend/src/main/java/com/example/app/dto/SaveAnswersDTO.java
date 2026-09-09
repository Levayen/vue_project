package com.example.app.dto;

import java.util.Map;

/**
 * 暂存答案 DTO（SPEC-exam-session）
 * key 为 paperQuestionId，value 为答案编码。
 */
public record SaveAnswersDTO(
        Map<Long, String> answers
) {
}
