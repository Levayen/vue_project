package com.example.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 阅卷工作台：某场考试下的待评学生答卷（M7 人工阅卷）
 * @param attemptId 尝试 id
 * @param studentId 学生 id
 * @param studentNumber 学号
 * @param studentName 姓名
 * @param className 班级名
 * @param pendingQuestions 待评主观题数
 * @param temporaryScore 客观题临时分
 * @param submitTime 交卷时间
 */
public record PendingAttemptDTO(
        Long attemptId,
        Long studentId,
        String studentNumber,
        String studentName,
        String className,
        Long pendingQuestions,
        BigDecimal temporaryScore,
        LocalDateTime submitTime) {}
