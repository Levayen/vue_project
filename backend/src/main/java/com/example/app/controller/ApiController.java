package com.example.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API根目录控制器
 * 提供API文档和端点列表
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * 获取API根信息
     * 返回系统名称、版本、描述和所有可用端点
     * GET /api
     * @return API信息和端点列表
     */
    @GetMapping
    public Map<String, Object> getApiRoot() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "学生管理系统 API");
        response.put("version", "1.0.0");
        response.put("description", "学生管理系统 RESTful API");
        
        List<Map<String, String>> endpoints = Arrays.asList(
            createEndpoint("GET", "/api/students", "获取学生列表"),
            createEndpoint("POST", "/api/students", "添加学生"),
            createEndpoint("GET", "/api/students/{id}", "获取学生详情"),
            createEndpoint("PUT", "/api/students/{id}", "更新学生信息"),
            createEndpoint("DELETE", "/api/students/{id}", "删除学生"),
            createEndpoint("GET", "/api/students/class/{classId}", "按班级查询学生"),
            createEndpoint("GET", "/api/students/search?name=xxx", "按姓名搜索学生"),
            createEndpoint("GET", "/api/classes", "获取班级列表"),
            createEndpoint("POST", "/api/classes", "添加班级"),
            createEndpoint("GET", "/api/classes/{id}", "获取班级详情"),
            createEndpoint("PUT", "/api/classes/{id}", "更新班级信息"),
            createEndpoint("DELETE", "/api/classes/{id}", "删除班级"),
            createEndpoint("GET", "/api/courses", "获取课程列表"),
            createEndpoint("POST", "/api/courses", "添加课程"),
            createEndpoint("GET", "/api/courses/{id}", "获取课程详情"),
            createEndpoint("PUT", "/api/courses/{id}", "更新课程信息"),
            createEndpoint("DELETE", "/api/courses/{id}", "删除课程"),
            createEndpoint("GET", "/api/enrollments", "获取所有选课记录"),
            createEndpoint("POST", "/api/enrollments", "选课"),
            createEndpoint("GET", "/api/enrollments/student/{studentId}", "查询学生选课"),
            createEndpoint("GET", "/api/enrollments/course/{courseId}", "查询课程学生"),
            createEndpoint("PUT", "/api/enrollments/{id}/grade", "更新成绩"),
            createEndpoint("DELETE", "/api/enrollments?studentId=&courseId=", "退课")
        );
        
        response.put("endpoints", endpoints);
        
        return response;
    }
    
    /**
     * 创建端点描述对象
     * @param method HTTP方法
     * @param path 端点路径
     * @param description 端点描述
     * @return 端点信息Map
     */
    private Map<String, String> createEndpoint(String method, String path, String description) {
        Map<String, String> endpoint = new HashMap<>();
        endpoint.put("method", method);
        endpoint.put("path", path);
        endpoint.put("description", description);
        return endpoint;
    }
}