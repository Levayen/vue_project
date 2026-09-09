package com.example.app.repository;

import com.example.app.entity.AttemptAnswer;
import com.example.app.entity.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 判分明细数据访问层（SPEC-grading）
 */
@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    /**
     * 一次尝试的全部判分明细（按题目顺序）
     */
    List<AttemptAnswer> findByAttempt_IdOrderByPaperQuestion_IdAsc(Long attemptId);

    /**
     * 统计一次尝试中指定判分状态的题目数（M7：is_correct=3 待评阅题数）
     */
    long countByAttempt_IdAndIsCorrect(Long attemptId, Integer isCorrect);

    /**
     * 学生的错题明细（错题本：跨考试聚合 is_correct=0 的已交卷题目）
     */
    List<AttemptAnswer> findByAttempt_StudentIdAndAttempt_StatusAndIsCorrect(
            Long studentId, AttemptStatus status, Integer isCorrect);

    /**
     * 学生的错题明细（按课程筛选）
     */
    List<AttemptAnswer> findByAttempt_StudentIdAndAttempt_StatusAndIsCorrectAndAttempt_Exam_Course_Id(
            Long studentId, AttemptStatus status, Integer isCorrect, Long courseId);
}
