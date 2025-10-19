package com.example.app.repository;

import com.example.app.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 课程数据访问层接口
 * 继承JpaRepository，提供课程实体的CRUD操作
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    /**
     * 根据课程名称查询课程
     * @param courseName 课程名称
     * @return 课程信息
     */
    Optional<Course> findByCourseName(String courseName);
}