package com.example.app.repository;

import com.example.app.entity.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 试卷题目快照数据访问层（SPEC-exam-paper）
 */
@Repository
public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {

    /**
     * 按试卷取题目（按 seq 升序）
     */
    List<PaperQuestion> findByPaper_IdOrderBySeqAsc(Long paperId);

    /**
     * 题目是否被任何试卷快照引用（QuestionReferencePort 真实实现使用）
     */
    boolean existsByQuestionId(Long questionId);

    /**
     * 清空试卷题目（编辑草稿整卷替换、删除试卷时调用，需在事务内）
     */
    void deleteByPaper_Id(Long paperId);
}
