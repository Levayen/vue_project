package com.example.app.dto;

/**
 * 分数段人数（SPEC-results）
 * @param label 分数段标签，如 "[60-70)"
 * @param count 人数
 */
public record ScoreBucketDTO(String label, long count) {}
