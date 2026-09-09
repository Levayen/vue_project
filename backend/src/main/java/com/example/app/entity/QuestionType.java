package com.example.app.entity;

/**
 * 题型枚举（SPEC-question-bank / grading 共用，M7 扩展主观题）
 * SINGLE 单选 / MULTI 多选 / JUDGE 判断 / FILL 填空（客观题，自动判分）
 * SHORT_ANSWER 简答 / ESSAY 论述（主观题，学生文本作答，教师人工阅卷）
 */
public enum QuestionType {
    SINGLE,
    MULTI,
    JUDGE,
    FILL,
    SHORT_ANSWER,
    ESSAY;

    /**
     * 主观题：不参与自动判分，交卷后落 is_correct=3 待评阅，由教师人工评分
     */
    public boolean isSubjective() {
        return this == SHORT_ANSWER || this == ESSAY;
    }

    /**
     * 按名称解析题型，非法返回 null（调用方决定报错方式）
     */
    public static QuestionType parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return QuestionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
