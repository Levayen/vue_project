package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷实体（SPEC-exam-paper）
 * 对应 exam_paper 表；归属课程，总分冗余存储（组卷完成后计算）。
 * 试卷题目以 paper_question 快照行关联，考试/阅卷只读快照，与题库解耦。
 */
@Entity
@Table(name = "exam_paper")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExamPaper {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 试卷名称
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /**
     * 归属课程
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * 总分 = Σ paper_question.score，组卷完成后计算冗余
     */
    @Column(name = "total_score", precision = 6, scale = 1)
    private BigDecimal totalScore;

    /**
     * 组卷方式：MANUAL / RANDOM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "generate_type", nullable = false, length = 16)
    private PaperGenerateType generateType;

    /**
     * 状态：DRAFT / PUBLISHED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaperStatus status;

    /**
     * 创建教师用户 id
     */
    @Column(name = "create_by")
    private Long createBy;

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

    public ExamPaper() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public PaperGenerateType getGenerateType() {
        return generateType;
    }

    public void setGenerateType(PaperGenerateType generateType) {
        this.generateType = generateType;
    }

    public PaperStatus getStatus() {
        return status;
    }

    public void setStatus(PaperStatus status) {
        this.status = status;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
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
