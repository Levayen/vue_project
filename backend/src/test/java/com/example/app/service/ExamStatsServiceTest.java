package com.example.app.service;

import com.example.app.dto.ExamStatsDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExamStatsService 统计纯逻辑单测（SPEC-results 测试重点）
 * 覆盖：正常集合、空集合零值、及格边界（正好 60）、100 分落入 [90-100]。
 */
class ExamStatsServiceTest {

    private final ExamStatsService service = new ExamStatsService(null, null, null);

    @Test
    void summarizeNormalSet() {
        ExamStatsDTO stats = service.summarize(List.of(
                bd("50"), bd("65"), bd("75"), bd("85"), bd("95"), bd("100")));

        assertEquals(6, stats.attended());
        assertEquals(0, stats.avgScore().compareTo(new BigDecimal("78.3")));
        assertEquals(0, stats.maxScore().compareTo(bd("100")));
        assertEquals(0, stats.minScore().compareTo(bd("50")));
        // 及格 5/6 = 83.3%
        assertEquals(83.3, stats.passRate(), 0.001);
        // 分数段：[0-60)=1, [60-70)=1, [70-80)=1, [80-90)=1, [90-100]=2
        assertEquals(1, stats.buckets().get(0).count());
        assertEquals(1, stats.buckets().get(1).count());
        assertEquals(1, stats.buckets().get(2).count());
        assertEquals(1, stats.buckets().get(3).count());
        assertEquals(2, stats.buckets().get(4).count());
    }

    @Test
    void emptyScoresReturnZeros() {
        ExamStatsDTO stats = service.summarize(List.of());

        assertEquals(0, stats.attended());
        assertEquals(0, stats.avgScore().compareTo(BigDecimal.ZERO));
        assertEquals(0, stats.maxScore().compareTo(BigDecimal.ZERO));
        assertEquals(0, stats.minScore().compareTo(BigDecimal.ZERO));
        assertEquals(0.0, stats.passRate(), 0.001);
        assertEquals(5, stats.buckets().size());
        assertEquals(0, stats.buckets().stream().mapToLong(ScoreBucketCount()).sum());
    }

    @Test
    void exactlySixtyIsPassAndInFirstPassBucket() {
        ExamStatsDTO stats = service.summarize(List.of(bd("60")));

        assertEquals(1, stats.attended());
        assertEquals(100.0, stats.passRate(), 0.001);
        assertEquals(0, stats.avgScore().compareTo(bd("60")));
        assertEquals(1, stats.buckets().get(1).count()); // [60-70)
        assertEquals(0, stats.buckets().get(0).count());
    }

    @Test
    void hundredFallsIntoLastBucket() {
        ExamStatsDTO stats = service.summarize(List.of(bd("100"), bd("90")));

        assertEquals(2, stats.buckets().get(4).count());
        assertEquals(0, stats.buckets().get(3).count());
    }

    @Test
    void allFailPassRateZero() {
        ExamStatsDTO stats = service.summarize(List.of(bd("10"), bd("59.9")));

        assertEquals(0.0, stats.passRate(), 0.001);
        assertEquals(2, stats.buckets().get(0).count());
    }

    private static java.util.function.ToLongFunction<com.example.app.dto.ScoreBucketDTO> ScoreBucketCount() {
        return com.example.app.dto.ScoreBucketDTO::count;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
