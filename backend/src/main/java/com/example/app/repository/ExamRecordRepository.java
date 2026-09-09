package com.example.app.repository;

import com.example.app.entity.ExamRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 有效成绩聚合数据访问层（SPEC-grading）
 */
@Repository
public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {

    /**
     * 某学生某场考试的有效成绩（upsert 定位）
     */
    Optional<ExamRecord> findByExam_IdAndStudentId(Long examId, Long studentId);

    /**
     * 一场考试的全部成绩（教师统计，按成绩降序）
     */
    List<ExamRecord> findByExam_IdOrderByTotalScoreDesc(Long examId);

    /**
     * 学生的全部有效成绩（成绩列表）
     */
    List<ExamRecord> findByStudentIdOrderByUpdateTimeDesc(Long studentId);
}
