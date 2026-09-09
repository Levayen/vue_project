package com.example.app.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 阅卷工作台：单份答卷的主观题评分详情（M7 人工阅卷）
 * @param attemptId 尝试 id
 * @param examId 考试 id
 * @param examTitle 考试名
 * @param studentNumber 学号
 * @param studentName 姓名
 * @param className 班级名
 * @param paperTotalScore 试卷满分
 * @param currentScore 当前总分（阅卷中为客观题临时分 + 已评主观题）
 * @param finalized 是否已经定稿（无待评题）
 * @param answers 主观题作答明细
 */
public record ReviewAttemptDTO(
        Long attemptId,
        Long examId,
        String examTitle,
        String studentNumber,
        String studentName,
        String className,
        BigDecimal paperTotalScore,
        BigDecimal currentScore,
        boolean finalized,
        List<SubjectiveAnswerDTO> answers) {

    /**
     * 单道主观题的评分信息
     * @param answerId 作答明细 id（评分接口入参）
     * @param type 题型：SHORT_ANSWER / ESSAY
     * @param content 题干
     * @param referenceAnswer 参考答案
     * @param answerText 学生作答原文（未答为 null）
     * @param fullScore 本题满分
     * @param score 当前得分（未评为 null）
     * @param correctFlag 3 待评阅 / 1 满分（正确）/ 0 未满分
     * @param teacherComment 教师评语
     */
    public record SubjectiveAnswerDTO(
            Long answerId,
            String type,
            String content,
            String referenceAnswer,
            String answerText,
            BigDecimal fullScore,
            BigDecimal score,
            Integer correctFlag,
            String teacherComment) {}
}
