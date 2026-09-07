package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * 课程实体类
 * 对应数据库表 course
 */
@Entity
@Table(name = "course")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Course {
    
    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 课程名称，非空且唯一
     */
    @NotBlank(message = "课程名称不能为空")
    @Column(name = "course_name", nullable = false, unique = true)
    private String courseName;
    
    /**
     * 学分
     */
    @Column(name = "credits")
    private Integer credits;
    
    /**
     * 课程描述
     */
    @Column(name = "description")
    private String description;
    
    /**
     * 默认构造函数
     */
    public Course() {}
    
    /**
     * 全参数构造函数
     * @param id 主键ID
     * @param courseName 课程名称
     * @param credits 学分
     * @param description 课程描述
     */
    public Course(Long id, String courseName, Integer credits, String description) {
        this.id = id;
        this.courseName = courseName;
        this.credits = credits;
        this.description = description;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public Integer getCredits() {
        return credits;
    }
    
    public void setCredits(Integer credits) {
        this.credits = credits;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}