package com.example.app.controller;

import com.example.app.entity.Enrollment;
import com.example.app.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 选课管理控制器
 * 提供学生选课相关的REST API接口
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    /**
     * 获取所有选课记录
     * GET /api/enrollments
     * @return 选课记录列表
     */
    @GetMapping
    public List<Enrollment> getAllEnrollments() {
        return enrollmentService.getAllEnrollments();
    }
    
    /**
     * 根据学生ID获取选课记录
     * GET /api/enrollments/student/{studentId}
     * @param studentId 学生ID
     * @return 选课记录列表
     */
    @GetMapping("/student/{studentId}")
    public List<Enrollment> getEnrollmentsByStudent(@PathVariable Long studentId) {
        return enrollmentService.getEnrollmentsByStudent(studentId);
    }
    
    /**
     * 根据课程ID获取选课记录
     * GET /api/enrollments/course/{courseId}
     * @param courseId 课程ID
     * @return 选课记录列表
     */
    @GetMapping("/course/{courseId}")
    public List<Enrollment> getEnrollmentsByCourse(@PathVariable Long courseId) {
        return enrollmentService.getEnrollmentsByCourse(courseId);
    }
    
    /**
     * 学生选课
     * POST /api/enrollments
     * 请求体: {"studentId": 1, "courseId": 1}
     * @param request 包含studentId和courseId的请求体
     * @return 选课记录或400
     */
    @PostMapping
    public ResponseEntity<Enrollment> enrollCourse(@RequestBody Map<String, Long> request) {
        try {
            Long studentId = request.get("studentId");
            Long courseId = request.get("courseId");
            return ResponseEntity.ok(enrollmentService.enrollCourse(studentId, courseId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新成绩
     * PUT /api/enrollments/{id}/grade
     * 请求体: {"grade": 95.5}
     * @param id 选课记录ID
     * @param request 包含grade的请求体
     * @return 更新后的选课记录或404
     */
    @PutMapping("/{id}/grade")
    public ResponseEntity<Enrollment> updateGrade(@PathVariable Long id, @RequestBody Map<String, Double> request) {
        try {
            Double grade = request.get("grade");
            return ResponseEntity.ok(enrollmentService.updateGrade(id, grade));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 退课
     * DELETE /api/enrollments?studentId=1&courseId=1
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 200 OK
     */
    @DeleteMapping
    public ResponseEntity<Void> dropCourse(@RequestParam Long studentId, @RequestParam Long courseId) {
        enrollmentService.dropCourse(studentId, courseId);
        return ResponseEntity.ok().build();
    }
}