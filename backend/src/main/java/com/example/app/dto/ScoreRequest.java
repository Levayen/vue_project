package com.example.app.dto;

import java.math.BigDecimal;

/**
 * 主观题评分请求（M7 人工阅卷）
 * @param score 本题得分（0 ~ 满分，服务端按题目满分校验）
 * @param comment 教师评语（可空）
 */
public record ScoreRequest(
        BigDecimal score,
        String comment) {}
