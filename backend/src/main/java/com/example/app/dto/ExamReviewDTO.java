package com.example.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单次考试回顾（SPEC-results）
 * @param examId 考试 id
 * @param title 考试名
 * @param courseName 课程名
 * @param score 有效成绩
 * @param fullScore 试卷总分
 * @param submitTime 交卷时间
 * @param questions 逐题回顾
 */
public record ExamReviewDTO(
        Long examId,
        String title,
        String courseName,
        BigDecimal score,
        BigDecimal fullScore,
        LocalDateTime submitTime,
        List<QuestionReviewDTO> questions) {}
