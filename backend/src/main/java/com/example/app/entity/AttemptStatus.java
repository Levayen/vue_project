package com.example.app.entity;

/**
 * 考试尝试状态（SPEC-exam-session + M7 人工阅卷）
 * IN_PROGRESS 进行中；
 * PENDING_REVIEW 已交卷、含待人工评阅的主观题（成绩未定，占用考试次数）；
 * SUBMITTED 判分完成（纯客观题自动判分，或主观题已全部人工评阅）。
 */
public enum AttemptStatus {
    IN_PROGRESS,
    PENDING_REVIEW,
    SUBMITTED
}
