package com.example.app.controller;

import com.example.app.entity.Student;
import com.example.app.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生管理控制器
 * 提供学生相关的REST API接口
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {
    
    @Autowired
    private StudentService studentService;
    
    /**
     * 获取所有学生列表
     * GET /api/students
     * @return 学生列表
     */
    @GetMapping
    public List<Student> getAllStudents() {
        return studentService.getAllStudents();
    }
    
    /**
     * 根据ID获取学生信息
     * GET /api/students/{id}
     * @param id 学生ID
     * @return 学生信息或404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建学生
     * POST /api/students
     * @param student 学生信息
     * @return 创建后的学生信息
     */
    @PostMapping
    public ResponseEntity<Student> createStudent(@Valid @RequestBody Student student) {
        try {
            return ResponseEntity.ok(studentService.createStudent(student));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 更新学生信息
     * PUT /api/students/{id}
     * @param id 学生ID
     * @param studentDetails 更新的学生信息
     * @return 更新后的学生信息或404
     */
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @Valid @RequestBody Student studentDetails) {
        try {
            return ResponseEntity.ok(studentService.updateStudent(id, studentDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除学生
     * DELETE /api/students/{id}
     * @param id 学生ID
     * @return 200 OK
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 根据班级ID获取学生列表
     * GET /api/students/class/{classId}
     * @param classId 班级ID
     * @return 学生列表
     */
    @GetMapping("/class/{classId}")
    public List<Student> getStudentsByClass(@PathVariable Long classId) {
        return studentService.getStudentsByClass(classId);
    }
    
    /**
     * 根据姓名模糊搜索学生
     * GET /api/students/search?name=xxx
     * @param name 姓名关键词
     * @return 学生列表
     */
    @GetMapping("/search")
    public List<Student> searchStudentsByName(@RequestParam String name) {
        return studentService.searchStudentsByName(name);
    }
}