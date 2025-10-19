package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学生管理系统启动类
 * Spring Boot 应用入口
 */
@SpringBootApplication
public class Application {
    
    /**
     * 应用主入口方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}