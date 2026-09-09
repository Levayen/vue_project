package com.example.app.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考试统计（SPEC-results）
 * @param attended 参考人数（有有效成绩即参考）
 * @param avgScore 均分（1 位小数）
 * @param maxScore 最高分
 * @param minScore 最低分
 * @param passRate 及格率百分比（≥60，1 位小数）
 * @param buckets 分数段人数分布 [0-60)/[60-70)/[70-80)/[80-90)/[90-100]
 */
public record ExamStatsDTO(
        long attended,
        BigDecimal avgScore,
        BigDecimal maxScore,
        BigDecimal minScore,
        double passRate,
        List<ScoreBucketDTO> buckets) {}
