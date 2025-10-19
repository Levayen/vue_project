package com.example.app.service;

import com.example.app.entity.Course;
import com.example.app.entity.Enrollment;
import com.example.app.entity.Student;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.EnrollmentRepository;
import com.example.app.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 选课业务逻辑层
 * 处理学生选课相关的业务操作，包括选课、退课、成绩管理
 */
@Service
public class EnrollmentService {
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    /**
     * 获取所有选课记录
     * @return 选课记录列表
     */
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }
    
    /**
     * 根据学生ID获取选课记录列表
     * @param studentId 学生ID
     * @return 选课记录列表
     */
    public List<Enrollment> getEnrollmentsByStudent(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }
    
    /**
     * 根据课程ID获取选课记录列表
     * @param courseId 课程ID
     * @return 选课记录列表
     */
    public List<Enrollment> getEnrollmentsByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }
    
    /**
     * 学生选课
     * 验证学生和课程是否存在，检查是否已选该课程，然后创建选课记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课记录
     */
    public Enrollment enrollCourse(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        
        Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existingEnrollment.isPresent()) {
            throw new RuntimeException("Student already enrolled in this course");
        }
        
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        
        return enrollmentRepository.save(enrollment);
    }
    
    /**
     * 更新成绩
     * 根据选课记录ID查找记录并更新成绩
     * @param enrollmentId 选课记录ID
     * @param grade 成绩
     * @return 更新后的选课记录
     */
    public Enrollment updateGrade(Long enrollmentId, Double grade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found with id: " + enrollmentId));
        
        enrollment.setGrade(grade);
        return enrollmentRepository.save(enrollment);
    }
    
    /**
     * 退课
     * 根据学生ID和课程ID删除选课记录
     * @param studentId 学生ID
     * @param courseId 课程ID
     */
    public void dropCourse(Long studentId, Long courseId) {
        enrollmentRepository.deleteByStudentIdAndCourseId(studentId, courseId);
    }
}