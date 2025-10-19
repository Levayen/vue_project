package com.example.app.controller;

import com.example.app.entity.Course;
import com.example.app.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程管理控制器
 * 提供课程相关的REST API接口
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    /**
     * 获取所有课程列表
     * GET /api/courses
     * @return 课程列表
     */
    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }
    
    /**
     * 根据ID获取课程信息
     * GET /api/courses/{id}
     * @param id 课程ID
     * @return 课程信息或404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建课程
     * POST /api/courses
     * @param course 课程信息
     * @return 创建后的课程信息
     */
    @PostMapping
    public Course createCourse(@Valid @RequestBody Course course) {
        return courseService.createCourse(course);
    }
    
    /**
     * 更新课程信息
     * PUT /api/courses/{id}
     * @param id 课程ID
     * @param courseDetails 更新的课程信息
     * @return 更新后的课程信息或404
     */
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @Valid @RequestBody Course courseDetails) {
        try {
            return ResponseEntity.ok(courseService.updateCourse(id, courseDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除课程
     * DELETE /api/courses/{id}
     * @param id 课程ID
     * @return 200 OK
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok().build();
    }
}