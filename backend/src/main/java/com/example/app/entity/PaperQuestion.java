package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 试卷题目快照实体（SPEC-exam-paper）
 * 对应 paper_question 表：试卷-题目关联 + 冗余快照字段。
 * 快照在组卷时写入；考试与阅卷全部读快照表，与题库解耦，
 * 题库后续修改/删除不影响已发布试卷。
 */
@Entity
@Table(name = "paper_question")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaperQuestion {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属试卷
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private ExamPaper paper;

    /**
     * 题库题目 id（保留来源引用；内容以快照为准）
     */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /**
     * 题目顺序（同卷不重复，从 1 开始）
     */
    @Column(name = "seq", nullable = false)
    private Integer seq;

    /**
     * 本题在本卷分值（可覆盖题目默认分值）
     */
    @Column(name = "score", nullable = false, precision = 5, scale = 1)
    private BigDecimal score;

    /**
     * 题干快照
     */
    @Column(name = "content_snapshot", nullable = false, columnDefinition = "text")
    private String contentSnapshot;

    /**
     * 选项快照 JSON（判断题/填空题为空）
     */
    @Column(name = "options_snapshot", columnDefinition = "text")
    private String optionsSnapshot;

    /**
     * 正确答案快照（判分依据；发卷时按 exam.open_book 决定是否下发）
     */
    @Column(name = "answer_snapshot", nullable = false, length = 255)
    private String answerSnapshot;

    /**
     * 题型快照：SINGLE / MULTI / JUDGE / FILL
     */
    @Column(name = "type_snapshot", nullable = false, length = 16)
    private String typeSnapshot;

    /**
     * 答案解析快照（开卷考试下发、闭卷交卷后回顾用）
     */
    @Column(name = "analysis_snapshot", columnDefinition = "text")
    private String analysisSnapshot;

    /**
     * 主观题参考答案快照（M7）：简答/论述阅卷参考，可为空
     */
    @Column(name = "reference_answer_snapshot", columnDefinition = "text")
    private String referenceAnswerSnapshot;

    public PaperQuestion() {}

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

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public void setContentSnapshot(String contentSnapshot) {
        this.contentSnapshot = contentSnapshot;
    }

    public String getOptionsSnapshot() {
        return optionsSnapshot;
    }

    public void setOptionsSnapshot(String optionsSnapshot) {
        this.optionsSnapshot = optionsSnapshot;
    }

    public String getAnswerSnapshot() {
        return answerSnapshot;
    }

    public void setAnswerSnapshot(String answerSnapshot) {
        this.answerSnapshot = answerSnapshot;
    }

    public String getReferenceAnswerSnapshot() {
        return referenceAnswerSnapshot;
    }

    public void setReferenceAnswerSnapshot(String referenceAnswerSnapshot) {
        this.referenceAnswerSnapshot = referenceAnswerSnapshot;
    }

    public String getTypeSnapshot() {
        return typeSnapshot;
    }

    public void setTypeSnapshot(String typeSnapshot) {
        this.typeSnapshot = typeSnapshot;
    }

    public String getAnalysisSnapshot() {
        return analysisSnapshot;
    }

    public void setAnalysisSnapshot(String analysisSnapshot) {
        this.analysisSnapshot = analysisSnapshot;
    }
}
