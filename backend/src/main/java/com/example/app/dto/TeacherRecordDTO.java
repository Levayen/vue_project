package com.example.app.dto;

import java.math.BigDecimal;

/**
 * 教师端成绩列表项（SPEC-results + M7 人工阅卷）
 * @param studentId 学生 id
 * @param studentNumber 学号
 * @param studentName 姓名
 * @param className 班级名
 * @param totalScore 有效成绩（pending=true 时为客观题临时分）
 * @param attemptCount 已提交次数
 * @param violationCount 违规次数（该生本场考试全部尝试累计）
 * @param pending 成绩是否待定（M7：含待教师评阅的主观题）
 */
public record TeacherRecordDTO(
        Long studentId,
        String studentNumber,
        String studentName,
        String className,
        BigDecimal totalScore,
        Integer attemptCount,
        Integer violationCount,
        Boolean pending) {}
