package com.example.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器
 * 提供根路径访问
 */
@RestController
public class HelloController {
    
    /**
     * 首页欢迎信息
     * GET /
     * @return 欢迎消息
     */
    @GetMapping("/")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}