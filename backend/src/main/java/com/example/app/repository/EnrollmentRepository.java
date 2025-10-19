package com.example.app.repository;

import com.example.app.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 选课数据访问层接口
 * 继承JpaRepository，提供选课实体的CRUD操作
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    /**
     * 根据学生ID查询选课列表
     * @param studentId 学生ID
     * @return 选课列表
     */
    List<Enrollment> findByStudentId(Long studentId);
    
    /**
     * 根据课程ID查询选课列表
     * @param courseId 课程ID
     * @return 选课列表
     */
    List<Enrollment> findByCourseId(Long courseId);
    
    /**
     * 根据学生ID和课程ID查询选课记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课记录
     */
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    
    /**
     * 根据学生ID和课程ID删除选课记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     */
    void deleteByStudentIdAndCourseId(Long studentId, Long courseId);
}