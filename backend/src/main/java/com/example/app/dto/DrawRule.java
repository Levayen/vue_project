package com.example.app.dto;

import java.math.BigDecimal;

/**
 * 随机抽题规则（SPEC-exam-paper）
 * 按题型 + 难度（null 不限）+ 数量 + 每题分值，从课程题库随机抽取。
 *
 * @param type             题型 SINGLE/MULTI/JUDGE/FILL
 * @param difficulty       难度 1/2/3，null 表示不限
 * @param count            抽题数量
 * @param scorePerQuestion 每题分值
 */
public record DrawRule(
        String type,
        Integer difficulty,
        Integer count,
        BigDecimal scorePerQuestion
) {
}
