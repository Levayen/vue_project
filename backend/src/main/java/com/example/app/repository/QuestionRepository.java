package com.example.app.repository;

import com.example.app.entity.Question;
import com.example.app.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 题库数据访问层（SPEC-question-bank）
 * 动态条件查询使用 JpaSpecificationExecutor（课程/题型/难度/关键词）。
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {

    /**
     * 随机抽题取池用（SPEC-exam-paper）：课程 + 题型的全部题目，按 id 升序保证输入稳定
     */
    List<Question> findByCourse_IdAndTypeOrderByIdAsc(Long courseId, QuestionType type);
}
