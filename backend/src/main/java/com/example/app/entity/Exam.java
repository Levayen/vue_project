package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 考试实体（SPEC-exam-session）
 * 对应 exam 表：基于已发布试卷的一场考试，含时间窗口、时长、次数、开卷/闭卷模式等规则。
 */
@Entity
@Table(name = "exam")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Exam {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联试卷（须 PUBLISHED 状态）
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paper_id", nullable = false)
    private ExamPaper paper;

    /**
     * 冗余自试卷课程，便于查询
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * 考试名称
     */
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /**
     * 时间窗口起
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 时间窗口止（晚于此不可入场/自动截止）
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * 考试时长（分钟），用于倒计时
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /**
     * 最大考试次数
     */
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 1;

    /**
     * 成绩规则：BEST 取最好成绩 / LAST 取末次成绩
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "score_rule", nullable = false, length = 8)
    private ScoreRule scoreRule = ScoreRule.LAST;

    /**
     * 是否打乱题目顺序
     */
    @Column(name = "shuffle", nullable = false)
    private Boolean shuffle = true;

    /**
     * 开卷模式：1=开卷（练习），发卷下发答案/解析；0=闭卷（正式），发卷脱敏，交卷后才可见答案
     */
    @Column(name = "open_book", nullable = false)
    private Boolean openBook = false;

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

    public Exam() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamPaper getPaper() {
        return paper;
    }

    public void setPaper(ExamPaper paper) {
        this.paper = paper;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public ScoreRule getScoreRule() {
        return scoreRule;
    }

    public void setScoreRule(ScoreRule scoreRule) {
        this.scoreRule = scoreRule;
    }

    public Boolean getShuffle() {
        return shuffle;
    }

    public void setShuffle(Boolean shuffle) {
        this.shuffle = shuffle;
    }

    public Boolean getOpenBook() {
        return openBook;
    }

    public void setOpenBook(Boolean openBook) {
        this.openBook = openBook;
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
