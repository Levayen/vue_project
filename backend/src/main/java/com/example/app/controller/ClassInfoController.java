package com.example.app.controller;

import com.example.app.entity.ClassInfo;
import com.example.app.service.ClassInfoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班级管理控制器
 * 提供班级相关的REST API接口
 */
@RestController
@RequestMapping("/api/classes")
public class ClassInfoController {
    
    @Autowired
    private ClassInfoService classInfoService;
    
    /**
     * 获取所有班级列表
     * GET /api/classes
     * @return 班级列表
     */
    @GetMapping
    public List<ClassInfo> getAllClasses() {
        return classInfoService.getAllClasses();
    }
    
    /**
     * 根据ID获取班级信息
     * GET /api/classes/{id}
     * @param id 班级ID
     * @return 班级信息或404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClassInfo> getClassById(@PathVariable Long id) {
        return classInfoService.getClassById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建班级
     * POST /api/classes
     * @param classInfo 班级信息
     * @return 创建后的班级信息
     */
    @PostMapping
    public ClassInfo createClass(@Valid @RequestBody ClassInfo classInfo) {
        return classInfoService.createClass(classInfo);
    }
    
    /**
     * 更新班级信息
     * PUT /api/classes/{id}
     * @param id 班级ID
     * @param classDetails 更新的班级信息
     * @return 更新后的班级信息或404
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClassInfo> updateClass(@PathVariable Long id, @Valid @RequestBody ClassInfo classDetails) {
        try {
            return ResponseEntity.ok(classInfoService.updateClass(id, classDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除班级
     * DELETE /api/classes/{id}
     * @param id 班级ID
     * @return 200 OK
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        classInfoService.deleteClass(id);
        return ResponseEntity.ok().build();
    }
}