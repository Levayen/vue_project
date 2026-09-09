package com.example.app.repository;

import com.example.app.entity.ExamPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 试卷数据访问层（SPEC-exam-paper）
 */
@Repository
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {

    /**
     * 按课程分页查询试卷；courseId 为空时由 Service 走 findAll
     */
    Page<ExamPaper> findByCourse_Id(Long courseId, Pageable pageable);
}
