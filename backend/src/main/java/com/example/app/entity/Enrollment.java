package com.example.app.entity;

import jakarta.persistence.*;

/**
 * 选课实体类
 * 对应数据库表 student_course，记录学生选课信息
 */
@Entity
@Table(name = "student_course")
public class Enrollment {
    
    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 学生，多对一关系，懒加载
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    /**
     * 课程，多对一关系，懒加载
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    /**
     * 成绩
     */
    @Column(name = "grade")
    private Double grade;
    
    /**
     * 默认构造函数
     */
    public Enrollment() {}
    
    /**
     * 全参数构造函数
     * @param id 主键ID
     * @param student 学生
     * @param course 课程
     * @param grade 成绩
     */
    public Enrollment(Long id, Student student, Course course, Double grade) {
        this.id = id;
        this.student = student;
        this.course = course;
        this.grade = grade;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Student getStudent() {
        return student;
    }
    
    public void setStudent(Student student) {
        this.student = student;
    }
    
    public Course getCourse() {
        return course;
    }
    
    public void setCourse(Course course) {
        this.course = course;
    }
    
    public Double getGrade() {
        return grade;
    }
    
    public void setGrade(Double grade) {
        this.grade = grade;
    }
}