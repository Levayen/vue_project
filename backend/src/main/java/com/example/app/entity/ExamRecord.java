package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 有效成绩聚合实体（SPEC-grading）
 * 对应 exam_record 表：一个学生一场考试一行，按成绩规则（BEST/LAST）聚合 attempt 分数。
 * 交卷判分后 upsert；唯一约束 (exam_id, student_id)。
 */
@Entity
@Table(name = "exam_record", uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "student_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExamRecord {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联考试
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    /**
     * 学生 id（关联 student.id）
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * 有效成绩（BEST 为历史最高，LAST 为末次）
     */
    @Column(name = "total_score", nullable = false, precision = 6, scale = 1)
    private BigDecimal totalScore;

    /**
     * BEST 规则下对应最高分 attempt
     */
    @Column(name = "best_attempt_id")
    private Long bestAttemptId;

    /**
     * LAST 规则下对应末次 attempt
     */
    @Column(name = "last_attempt_id")
    private Long lastAttemptId;

    /**
     * 已提交次数
     */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /**
     * 成绩是否待定（M7）：0 已定稿 / 1 存在待教师评阅的主观题答卷，totalScore 为临时分
     */
    @Column(name = "pending", nullable = false)
    private Integer pending = 0;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.updateTime = LocalDateTime.now();
    }

    public ExamRecord() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public Long getBestAttemptId() {
        return bestAttemptId;
    }

    public void setBestAttemptId(Long bestAttemptId) {
        this.bestAttemptId = bestAttemptId;
    }

    public Long getLastAttemptId() {
        return lastAttemptId;
    }

    public void setLastAttemptId(Long lastAttemptId) {
        this.lastAttemptId = lastAttemptId;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getPending() {
        return pending;
    }

    public void setPending(Integer pending) {
        this.pending = pending;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
