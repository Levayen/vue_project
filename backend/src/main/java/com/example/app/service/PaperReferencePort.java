package com.example.app.service;

/**
 * 试卷被考试引用检查端口（SPEC-exam-paper）
 * 用于解耦试卷与考试模块：exam-session 模块（M3）提供真实实现（查 exam 表），
 * 在此之前由默认桩实现返回"未被引用"。
 */
public interface PaperReferencePort {

    /**
     * 试卷是否已被任何考试引用
     */
    boolean isReferencedByExam(Long paperId);
}
