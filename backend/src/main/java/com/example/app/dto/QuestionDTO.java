package com.example.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 题目数据传输对象（SPEC-question-bank + M7 主观题）
 * 入参/出参共用；options 为结构化选项列表（实体中存 JSON 字符串）。
 * 客观题 answer 为答案编码（必填，Service 层按题型校验）；
 * 主观题（简答/论述）answer 承载参考答案（选填），实体存入 referenceAnswer。
 */
public record QuestionDTO(
        Long id,
        @NotNull(message = "课程不能为空")
        Long courseId,
        String courseName,
        @NotBlank(message = "题型不能为空")
        String type,
        @NotBlank(message = "题干不能为空")
        String content,
        List<QuestionOption> options,
        String answer,
        BigDecimal score,
        Integer difficulty,
        String analysis
) {}
