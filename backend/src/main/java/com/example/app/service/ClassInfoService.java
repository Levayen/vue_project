package com.example.app.service;

import com.example.app.entity.ClassInfo;
import com.example.app.repository.ClassInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 班级业务逻辑层
 * 处理班级相关的业务操作，包括CRUD
 */
@Service
public class ClassInfoService {
    
    @Autowired
    private ClassInfoRepository classInfoRepository;
    
    /**
     * 获取所有班级列表
     * @return 班级列表
     */
    public List<ClassInfo> getAllClasses() {
        return classInfoRepository.findAll();
    }
    
    /**
     * 根据ID获取班级信息
     * @param id 班级ID
     * @return 班级信息（可选）
     */
    public Optional<ClassInfo> getClassById(Long id) {
        return classInfoRepository.findById(id);
    }
    
    /**
     * 创建班级
     * @param classInfo 班级信息
     * @return 创建后的班级信息
     */
    public ClassInfo createClass(ClassInfo classInfo) {
        return classInfoRepository.save(classInfo);
    }
    
    /**
     * 更新班级信息
     * 根据ID查找现有班级，更新字段值
     * @param id 班级ID
     * @param classDetails 更新的班级信息
     * @return 更新后的班级信息
     */
    public ClassInfo updateClass(Long id, ClassInfo classDetails) {
        ClassInfo existingClass = classInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + id));
        
        existingClass.setClassName(classDetails.getClassName());
        existingClass.setGrade(classDetails.getGrade());
        existingClass.setMajor(classDetails.getMajor());
        
        return classInfoRepository.save(existingClass);
    }
    
    /**
     * 删除班级
     * @param id 班级ID
     */
    public void deleteClass(Long id) {
        classInfoRepository.deleteById(id);
    }
}