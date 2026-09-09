package com.example.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 监考视图 DTO（M6 考试监控与监考 + M7 人工阅卷）
 * 一场考试的监考汇总统计 + 每位学生的参考状态、成绩、剩余时间、违规明细。
 */
public record MonitoringDTO(
        Long examId,
        String title,
        /** 选课参考人数 */
        Integer enrolledCount,
        /** 进行中人数 */
        Integer inProgressCount,
        /** 已交卷人数（判分完成） */
        Integer submittedCount,
        /** 待阅卷人数（M7：已交卷含主观题，等待教师评分） */
        Integer pendingReviewCount,
        /** 未开始人数 */
        Integer notStartedCount,
        /** 违规总人次 */
        Integer totalViolations,
        List<StudentAttemptInfo> students
) {
    /**
     * 单个学生的考试状态
     */
    public record StudentAttemptInfo(
            Long studentId,
            String studentName,
            String studentNumber,
            /** 未开始 / IN_PROGRESS / PENDING_REVIEW / SUBMITTED */
            String status,
            Integer attemptCount,
            BigDecimal score,
            Integer violationCount,
            /** 最新一次尝试的开考时间 */
            LocalDateTime startTime,
            /** 剩余秒数（进行中有值，其他为 null） */
            Long remainingSeconds,
            /** 违规事件明细 */
            List<ViolationEvent> violations
    ) {}

    /**
     * 单条违规事件
     */
    public record ViolationEvent(
            String type,
            String time
    ) {}
}
