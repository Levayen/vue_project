package com.example.app.dto;

/**
 * 违规上报 DTO（SPEC-exam-session）
 * type: VISIBILITY（切屏/失焦）/ COPY / PASTE / FULLSCREEN 等
 */
public record ViolationDTO(
        String type
) {
}
