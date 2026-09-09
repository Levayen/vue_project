package com.example.app.dto;

/**
 * 阅卷工作台：有待评答卷的考试（M7 人工阅卷）
 * @param examId 考试 id
 * @param examTitle 考试名
 * @param courseName 课程名
 * @param pendingAttempts 待评答卷数（学生数）
 * @param pendingQuestions 待评主观题总道次
 */
public record PendingExamDTO(
        Long examId,
        String examTitle,
        String courseName,
        Long pendingAttempts,
        Long pendingQuestions) {}
