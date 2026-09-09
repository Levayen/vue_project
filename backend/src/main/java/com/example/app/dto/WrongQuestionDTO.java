package com.example.app.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本条目（SPEC-results + M7 人工阅卷）
 * @param examId 考试 id
 * @param examTitle 考试名
 * @param courseName 课程名
 * @param type 题型：SINGLE/MULTI/JUDGE/FILL/SHORT_ANSWER/ESSAY
 * @param content 题干快照
 * @param options 选项快照
 * @param studentAnswer 我的答案（客观题为编码；主观题为作答原文；未答为 null）
 * @param correctAnswer 正确答案编码（主观题为空，参考答案见 referenceAnswer）
 * @param analysis 解析快照
 * @param score 本题实得分
 * @param fullScore 本题满分
 * @param submitTime 交卷时间
 * @param referenceAnswer 主观题参考答案（M7）
 */
public record WrongQuestionDTO(
        Long examId,
        String examTitle,
        String courseName,
        String type,
        String content,
        List<QuestionOption> options,
        String studentAnswer,
        String correctAnswer,
        String analysis,
        BigDecimal score,
        BigDecimal fullScore,
        LocalDateTime submitTime,
        String referenceAnswer) {}
