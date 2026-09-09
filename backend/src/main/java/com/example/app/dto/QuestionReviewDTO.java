package com.example.app.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考后回顾单题（SPEC-results + M7 人工阅卷）
 * @param type 题型：SINGLE/MULTI/JUDGE/FILL/SHORT_ANSWER/ESSAY
 * @param content 题干快照
 * @param options 选项快照（判断/填空/主观题为空）
 * @param studentAnswer 学生答案（客观题为编码；主观题为作答原文；未答为 null）
 * @param correctAnswer 正确答案编码（主观题为空，参考答案见 referenceAnswer）
 * @param correctFlag 0 错 / 1 对 / 3 待评阅（主观题未评）
 * @param analysis 答案解析快照
 * @param score 本题实得分
 * @param fullScore 本题满分
 * @param referenceAnswer 主观题参考答案（M7）
 * @param teacherComment 教师阅卷评语（M7，主观题评分后有值）
 */
public record QuestionReviewDTO(
        String type,
        String content,
        List<QuestionOption> options,
        String studentAnswer,
        String correctAnswer,
        Integer correctFlag,
        String analysis,
        BigDecimal score,
        BigDecimal fullScore,
        String referenceAnswer,
        String teacherComment) {}
