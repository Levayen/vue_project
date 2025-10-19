package com.example.app.repository;

import com.example.app.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生数据访问层接口
 * 继承JpaRepository，提供学生实体的CRUD操作
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    /**
     * 根据学号查询学生
     * @param studentNumber 学号
     * @return 学生信息
     */
    Optional<Student> findByStudentNumber(String studentNumber);
    
    /**
     * 根据班级ID查询学生列表
     * @param classId 班级ID
     * @return 学生列表
     */
    List<Student> findByClassInfoId(Long classId);
    
    /**
     * 根据姓名模糊查询学生列表
     * @param name 姓名
     * @return 学生列表
     */
    List<Student> findByNameContaining(String name);
}