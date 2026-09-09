package com.example.app.repository;

import com.example.app.entity.AttemptStatus;
import com.example.app.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 考试尝试数据访问层（SPEC-exam-session）
 */
@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /**
     * 该考试某学生的全部尝试（按开考时间倒序）
     */
    List<ExamAttempt> findByExam_IdAndStudentIdOrderByStartTimeDesc(Long examId, Long studentId);

    /**
     * 该考试某学生的进行中尝试（用于续考）
     */
    Optional<ExamAttempt> findByExam_IdAndStudentIdAndStatus(Long examId, Long studentId, AttemptStatus status);

    /**
     * 统计已提交次数（占用考试次数）
     */
    long countByExam_IdAndStudentIdAndStatus(Long examId, Long studentId, AttemptStatus status);

    /**
     * 统计已交卷次数（M7：SUBMITTED 与 PENDING_REVIEW 均占用考试次数，即非进行中）
     */
    long countByExam_IdAndStudentIdAndStatusNot(Long examId, Long studentId, AttemptStatus status);

    /**
     * 一场考试的全部尝试（监考视角）
     */
    List<ExamAttempt> findByExam_IdOrderByStartTimeDesc(Long examId);

    /**
     * 一场考试中指定状态的全部尝试（M7 阅卷队列：PENDING_REVIEW）
     */
    List<ExamAttempt> findByExam_IdAndStatus(Long examId, AttemptStatus status);

    /**
     * 全部考试中指定状态的尝试（M7 阅卷工作台汇总）
     */
    List<ExamAttempt> findByStatus(AttemptStatus status);
}
