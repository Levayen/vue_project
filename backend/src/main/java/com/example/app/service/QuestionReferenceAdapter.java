package com.example.app.service;

import com.example.app.repository.PaperQuestionRepository;
import org.springframework.stereotype.Component;

/**
 * 题目引用检查端口真实实现（SPEC-exam-paper）
 * 查询 paper_question 快照表；替换 M1 的 noop 桩（@ConditionalOnMissingBean 自动退让）。
 */
@Component
public class QuestionReferenceAdapter implements QuestionReferencePort {

    private final PaperQuestionRepository paperQuestionRepository;

    public QuestionReferenceAdapter(PaperQuestionRepository paperQuestionRepository) {
        this.paperQuestionRepository = paperQuestionRepository;
    }

    @Override
    public boolean isReferencedByPaper(Long questionId) {
        return paperQuestionRepository.existsByQuestionId(questionId);
    }
}
