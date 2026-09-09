package com.example.app.service;

import com.example.app.repository.ExamRepository;
import org.springframework.stereotype.Component;

/**
 * 试卷引用检查端口真实实现（SPEC-exam-session）
 * 查询 exam 表；替换 M2 的 noop 桩（@ConditionalOnMissingBean 自动退让）。
 */
@Component
public class PaperReferenceAdapter implements PaperReferencePort {

    private final ExamRepository examRepository;

    public PaperReferenceAdapter(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    public boolean isReferencedByExam(Long paperId) {
        return examRepository.existsByPaper_Id(paperId);
    }
}
