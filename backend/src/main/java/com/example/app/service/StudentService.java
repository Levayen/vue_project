package com.example.app.service;

import com.example.app.entity.ClassInfo;
import com.example.app.entity.Student;
import com.example.app.repository.ClassInfoRepository;
import com.example.app.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 学生业务逻辑层
 * 处理学生相关的业务操作，包括CRUD和关联处理
 */
@Service
public class StudentService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private ClassInfoRepository classInfoRepository;
    
    /**
     * 获取所有学生列表
     * @return 学生列表
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }
    
    /**
     * 根据ID获取学生信息
     * @param id 学生ID
     * @return 学生信息（可选）
     */
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }
    
    /**
     * 创建学生
     * 如果学生关联了班级，则从数据库查询班级实体并关联
     * @param student 学生信息
     * @return 创建后的学生信息
     */
    public Student createStudent(Student student) {
        if (student.getClassInfo() != null && student.getClassInfo().getId() != null) {
            ClassInfo classInfo = classInfoRepository.findById(student.getClassInfo().getId())
                    .orElseThrow(() -> new RuntimeException("Class not found with id: " + student.getClassInfo().getId()));
            student.setClassInfo(classInfo);
        }
        return studentRepository.save(student);
    }
    
    /**
     * 更新学生信息
     * 根据ID查找现有学生，更新字段值，并处理班级关联
     * @param id 学生ID
     * @param studentDetails 更新的学生信息
     * @return 更新后的学生信息
     */
    public Student updateStudent(Long id, Student studentDetails) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        
        existingStudent.setStudentNumber(studentDetails.getStudentNumber());
        existingStudent.setName(studentDetails.getName());
        existingStudent.setGender(studentDetails.getGender());
        existingStudent.setBirthDate(studentDetails.getBirthDate());
        existingStudent.setPhone(studentDetails.getPhone());
        existingStudent.setEmail(studentDetails.getEmail());
        
        if (studentDetails.getClassInfo() != null && studentDetails.getClassInfo().getId() != null) {
            ClassInfo classInfo = classInfoRepository.findById(studentDetails.getClassInfo().getId())
                    .orElseThrow(() -> new RuntimeException("Class not found with id: " + studentDetails.getClassInfo().getId()));
            existingStudent.setClassInfo(classInfo);
        }
        
        return studentRepository.save(existingStudent);
    }
    
    /**
     * 删除学生
     * @param id 学生ID
     */
    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }
    
    /**
     * 根据班级ID获取学生列表
     * @param classId 班级ID
     * @return 学生列表
     */
    public List<Student> getStudentsByClass(Long classId) {
        return studentRepository.findByClassInfoId(classId);
    }
    
    /**
     * 根据姓名模糊搜索学生
     * @param name 姓名关键词
     * @return 学生列表
     */
    public List<Student> searchStudentsByName(String name) {
        return studentRepository.findByNameContaining(name);
    }
}