package com.example.app.entity;

/**
 * 试卷状态（SPEC-exam-paper）
 * DRAFT 草稿（可编辑、可删除）；PUBLISHED 已发布（只读，可被考试引用）。
 */
public enum PaperStatus {
    DRAFT,
    PUBLISHED
}
