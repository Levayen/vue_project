package com.example.app.dto;

/**
 * 题目选项（SPEC-question-bank）
 * @param key 选项标识，如 A/B/C/D
 * @param text 选项内容
 */
public record QuestionOption(String key, String text) {}
