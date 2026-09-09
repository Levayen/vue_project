package com.example.app.repository;

import com.example.app.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 考试数据访问层（SPEC-exam-session）
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    /**
     * 按课程查询考试（教师视角）
     */
    List<Exam> findByCourse_IdOrderByStartTimeDesc(Long courseId);

    /**
     * 按课程列表查询考试（学生视角：仅显示其选课对应的考试）
     */
    List<Exam> findByCourse_IdInOrderByStartTimeDesc(List<Long> courseIds);

    /**
     * 试卷是否被考试引用（PaperReferencePort 真实实现使用）
     */
    boolean existsByPaper_Id(Long paperId);
}
