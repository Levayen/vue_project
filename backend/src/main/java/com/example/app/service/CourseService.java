package com.example.app.service;

import com.example.app.entity.Course;
import com.example.app.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 课程业务逻辑层
 * 处理课程相关的业务操作，包括CRUD
 */
@Service
public class CourseService {
    
    @Autowired
    private CourseRepository courseRepository;
    
    /**
     * 获取所有课程列表
     * @return 课程列表
     */
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
    
    /**
     * 根据ID获取课程信息
     * @param id 课程ID
     * @return 课程信息（可选）
     */
    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }
    
    /**
     * 创建课程
     * @param course 课程信息
     * @return 创建后的课程信息
     */
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }
    
    /**
     * 更新课程信息
     * 根据ID查找现有课程，更新字段值
     * @param id 课程ID
     * @param courseDetails 更新的课程信息
     * @return 更新后的课程信息
     */
    public Course updateCourse(Long id, Course courseDetails) {
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        
        existingCourse.setCourseName(courseDetails.getCourseName());
        existingCourse.setCredits(courseDetails.getCredits());
        existingCourse.setDescription(courseDetails.getDescription());
        
        return courseRepository.save(existingCourse);
    }
    
    /**
     * 删除课程
     * @param id 课程ID
     */
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}