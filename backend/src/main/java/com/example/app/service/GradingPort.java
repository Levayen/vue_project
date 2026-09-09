package com.example.app.service;

import com.example.app.entity.ExamAttempt;

import java.math.BigDecimal;

/**
 * 判分端口（SPEC-exam-session + M7 人工阅卷）
 * AttemptService 交卷时调用，计算本次尝试得分并返回。
 * 默认实现按客观题规则自动判分；主观题（简答/论述）落 is_correct=3 待评阅，
 * 此时 pendingReview=true，调用方应将尝试置为 PENDING_REVIEW，待教师人工阅卷后
 * 由 finalizeAttempt 重算总分并置 SUBMITTED。
 */
public interface GradingPort {

    /**
     * 判分结果
     *
     * @param score         本次尝试当前得分（纯客观题为最终分；含主观题时仅为客观题临时分）
     * @param pendingReview 是否存在待人工评阅的主观题（true 时成绩未定）
     */
    record GradeResult(BigDecimal score, boolean pendingReview) {
    }

    /**
     * 对已交卷尝试判分：客观题自动判分落库，主观题落待评阅；upsert 成绩记录
     *
     * @param attempt 已提交的尝试（含 answersJson）
     * @return 判分结果（得分 + 是否待人工阅卷）
     */
    GradeResult grade(ExamAttempt attempt);

    /**
     * 人工阅卷完成后定稿尝试（M7）：重算全部题目总分 → 尝试置 SUBMITTED →
     * 按成绩规则 upsert 成绩记录（含待定标记，若该生仍有其他待阅尝试则保持待定）
     *
     * @param attempt 主观题已全部评分的尝试
     * @return 最终总分
     */
    BigDecimal finalizeAttempt(ExamAttempt attempt);
}
