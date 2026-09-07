package com.example.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * 学生实体类
 * 对应数据库表 student
 */
@Entity
@Table(name = "student")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Student {
    
    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 学号，非空且唯一
     */
    @NotBlank(message = "学号不能为空")
    @Column(name = "student_number", nullable = false, unique = true)
    private String studentNumber;
    
    /**
     * 姓名，非空
     */
    @NotBlank(message = "姓名不能为空")
    @Column(name = "name", nullable = false)
    private String name;
    
    /**
     * 性别
     */
    @Column(name = "gender")
    private String gender;
    
    /**
     * 出生日期
     */
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    /**
     * 联系电话
     */
    @Column(name = "phone")
    private String phone;
    
    /**
     * 邮箱，需符合邮箱格式
     */
    @Email(message = "邮箱格式不正确")
    @Column(name = "email")
    private String email;
    
    /**
     * 所属班级，多对一关系，立即加载
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id")
    private ClassInfo classInfo;
    
    /**
     * 默认构造函数
     */
    public Student() {}
    
    /**
     * 全参数构造函数
     * @param id 主键ID
     * @param studentNumber 学号
     * @param name 姓名
     * @param gender 性别
     * @param birthDate 出生日期
     * @param phone 联系电话
     * @param email 邮箱
     * @param classInfo 所属班级
     */
    public Student(Long id, String studentNumber, String name, String gender, LocalDate birthDate, String phone, String email, ClassInfo classInfo) {
        this.id = id;
        this.studentNumber = studentNumber;
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.classInfo = classInfo;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getStudentNumber() {
        return studentNumber;
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public ClassInfo getClassInfo() {
        return classInfo;
    }
    
    public void setClassInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }
}