package com.example.app.controller;

import com.example.app.dto.ExamReviewDTO;
import com.example.app.dto.StudentResultDTO;
import com.example.app.dto.WrongQuestionDTO;
import com.example.app.service.ResultService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生成绩接口（SPEC-results）
 * 路径 /api/student/** 由 AuthInterceptor 强制 STUDENT 角色；
 * 数据归属由 ResultService 按 token 身份校验，只能查本人。
 */
@RestController
@RequestMapping("/api/student")
public class StudentResultController {

    private final ResultService resultService;

    public StudentResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /**
     * 我的成绩列表
     */
    @GetMapping("/results")
    public List<StudentResultDTO> myResults() {
        return resultService.myResults();
    }

    /**
     * 单次考试回顾（未交卷 404；仅本人）
     */
    @GetMapping("/results/{examId}/review")
    public ExamReviewDTO review(@PathVariable Long examId) {
        return resultService.review(examId);
    }

    /**
     * 错题本（跨考试聚合，可按课程筛选）
     */
    @GetMapping("/wrong-book")
    public List<WrongQuestionDTO> wrongBook(@RequestParam(required = false) Long courseId) {
        return resultService.wrongBook(courseId);
    }
}
