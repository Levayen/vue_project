package com.example.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生成绩列表项（SPEC-results + M7 人工阅卷）
 * @param examId 考试 id
 * @param examTitle 考试名
 * @param courseName 课程名
 * @param totalScore 有效成绩（按成绩规则聚合；pending=true 时为客观题临时分）
 * @param attemptCount 已提交次数
 * @param submitTime 有效成绩对应尝试的交卷时间
 * @param pass 是否及格（≥60）；pending=true 时无意义（成绩未定）
 * @param pending 成绩是否待定（M7：含待教师评阅的主观题）
 */
public record StudentResultDTO(
        Long examId,
        String examTitle,
        String courseName,
        BigDecimal totalScore,
        Integer attemptCount,
        LocalDateTime submitTime,
        boolean pass,
        boolean pending) {}
