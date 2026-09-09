package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题库题目实体（SPEC-question-bank）
 * 对应 question 表；归属课程，支持四种客观题型。
 * options 为选项 JSON（单选/多选使用），answer 遵循答案编码约定，判分模块共用。
 */
@Entity
@Table(name = "question")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Question {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 归属课程
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * 题型：SINGLE / MULTI / JUDGE / FILL / SHORT_ANSWER / ESSAY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private QuestionType type;

    /**
     * 题干
     */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /**
     * 选项 JSON，如 [{"key":"A","text":"..."}]；判断题/填空题为空
     */
    @Column(name = "options", columnDefinition = "text")
    private String options;

    /**
     * 正确答案（编码约定：单选 A / 多选 ABD / 判断 T|F / 填空 北京||北京市）
     * 主观题（简答/论述）无标准答案，固定空串；参考答案见 referenceAnswer。
     */
    @Column(name = "answer", nullable = false, length = 255)
    private String answer;

    /**
     * 主观题参考答案（M7）：简答/论述的阅卷参考，可为空
     */
    @Column(name = "reference_answer", columnDefinition = "text")
    private String referenceAnswer;

    /**
     * 默认分值
     */
    @Column(name = "score", nullable = false, precision = 5, scale = 1)
    private BigDecimal score = new BigDecimal("5");

    /**
     * 难度：1 易 / 2 中 / 3 难
     */
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty = 1;

    /**
     * 答案解析
     */
    @Column(name = "analysis", columnDefinition = "text")
    private String analysis;

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

    public Question() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
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
