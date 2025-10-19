package com.example.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * 班级实体类
 * 对应数据库表 class_info
 */
@Entity
@Table(name = "class_info")
public class ClassInfo {
    
    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 班级名称，非空且唯一
     */
    @NotBlank(message = "班级名称不能为空")
    @Column(name = "class_name", nullable = false, unique = true)
    private String className;
    
    /**
     * 年级
     */
    @Column(name = "grade")
    private String grade;
    
    /**
     * 专业
     */
    @Column(name = "major")
    private String major;
    
    /**
     * 默认构造函数
     */
    public ClassInfo() {}
    
    /**
     * 全参数构造函数
     * @param id 主键ID
     * @param className 班级名称
     * @param grade 年级
     * @param major 专业
     */
    public ClassInfo(Long id, String className, String grade, String major) {
        this.id = id;
        this.className = className;
        this.grade = grade;
        this.major = major;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getGrade() {
        return grade;
    }
    
    public void setGrade(String grade) {
        this.grade = grade;
    }
    
    public String getMajor() {
        return major;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
}