package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 每题判分明细实体（SPEC-grading）
 * 对应 attempt_answer 表：交卷判分时逐题落库，支撑考后回顾与错题本追溯。
 * is_correct：0 错 / 1 对（多选无半对；3 预留给未来主观题待评阅）。
 */
@Entity
@Table(name = "attempt_answer")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AttemptAnswer {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属考试尝试
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    /**
     * 试卷题目快照（含 content/options/analysis，回顾时读取）
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paper_question_id", nullable = false)
    private PaperQuestion paperQuestion;

    /**
     * 题型快照：SINGLE / MULTI / JUDGE / FILL / SHORT_ANSWER / ESSAY
     */
    @Column(name = "question_type", nullable = false, length = 16)
    private String questionType;

    /**
     * 学生答案编码（客观题使用；未作答为 null；主观题固定为 null，作答原文见 answerText）
     */
    @Column(name = "student_answer", length = 255)
    private String studentAnswer;

    /**
     * 主观题学生作答原文（M7）：简答/论述文本，长文本字段
     */
    @Column(name = "answer_text", columnDefinition = "mediumtext")
    private String answerText;

    /**
     * 正确答案快照（客观题判分依据；主观题固定为空串，参考答案读 paperQuestion.referenceAnswerSnapshot）
     */
    @Column(name = "correct_answer", nullable = false, length = 255)
    private String correctAnswer;

    /**
     * 判分结果：0 错 / 1 对 / 3 待评阅（主观题初始态）
     */
    @Column(name = "is_correct", nullable = false)
    private Integer isCorrect;

    /**
     * 教师阅卷评语（M7）：主观题人工评分时填写，可为空
     */
    @Column(name = "teacher_comment", columnDefinition = "text")
    private String teacherComment;

    /**
     * 本题实得分
     */
    @Column(name = "score", nullable = false, precision = 5, scale = 1)
    private BigDecimal score;

    /**
     * 本题满分
     */
    @Column(name = "full_score", nullable = false, precision = 5, scale = 1)
    private BigDecimal fullScore;

    public AttemptAnswer() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ExamAttempt attempt) {
        this.attempt = attempt;
    }

    public PaperQuestion getPaperQuestion() {
        return paperQuestion;
    }

    public void setPaperQuestion(PaperQuestion paperQuestion) {
        this.paperQuestion = paperQuestion;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Integer getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Integer isCorrect) {
        this.isCorrect = isCorrect;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getFullScore() {
        return fullScore;
    }

    public void setFullScore(BigDecimal fullScore) {
        this.fullScore = fullScore;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
    }
}
