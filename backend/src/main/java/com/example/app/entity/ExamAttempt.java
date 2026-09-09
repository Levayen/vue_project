package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生一次考试尝试实体（SPEC-exam-session）
 * 对应 exam_attempt 表：开考时刻、截止时刻、暂存答案、违规记录、判分结果。
 * 同一 (exam, student) 可有多次尝试；已提交次数才计入 max_attempts，进行中崩溃不占用次数。
 */
@Entity
@Table(name = "exam_attempt")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExamAttempt {

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
     * 状态：IN_PROGRESS / SUBMITTED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AttemptStatus status;

    /**
     * 实际开考时刻
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 本次截止 = min(start + duration, exam.end_time)，开考时计算
     */
    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    /**
     * 交卷时刻
     */
    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    /**
     * 暂存答案：{"<paperQuestionId>": "答案编码"}
     */
    @Column(name = "answers_json", columnDefinition = "mediumtext")
    private String answersJson;

    /**
     * 违规次数（切屏/失焦/粘贴等）
     */
    @Column(name = "violation_count", nullable = false)
    private Integer violationCount = 0;

    /**
     * 违规事件明细 JSON：[{"type":"VISIBILITY","time":"2026-..."}]
     */
    @Column(name = "violations_json", columnDefinition = "text")
    private String violationsJson;

    /**
     * 本次判分结果（交卷时由 GradingPort 计算）
     */
    @Column(name = "score", precision = 6, scale = 1)
    private BigDecimal score;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    public ExamAttempt() {}

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

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }

    public Integer getViolationCount() {
        return violationCount;
    }

    public void setViolationCount(Integer violationCount) {
        this.violationCount = violationCount;
    }

    public String getViolationsJson() {
        return violationsJson;
    }

    public void setViolationsJson(String violationsJson) {
        this.violationsJson = violationsJson;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
