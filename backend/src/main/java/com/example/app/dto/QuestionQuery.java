package com.example.app.dto;

/**
 * 题库分页查询条件（SPEC-question-bank）
 */
public record QuestionQuery(
        Long courseId,
        String type,
        Integer difficulty,
        String keyword,
        Integer page,
        Integer size
) {}
