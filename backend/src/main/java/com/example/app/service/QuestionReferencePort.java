package com.example.app.service;

/**
 * 题目被试卷引用检查端口（SPEC-question-bank）
 * 用于解耦题库与试卷模块：exam-paper 模块提供真实实现（查 paper_question），
 * 在此之前由默认桩实现返回"未被引用"。
 */
public interface QuestionReferencePort {

    /**
     * 题目是否被任何试卷快照引用
     */
    boolean isReferencedByPaper(Long questionId);
}
