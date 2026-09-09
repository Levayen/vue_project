package com.example.app.controller;

import com.example.app.dto.SaveAnswersDTO;
import com.example.app.dto.StartExamResponse;
import com.example.app.dto.ViolationDTO;
import com.example.app.service.AttemptService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生考试接口（SPEC-exam-session）
 * 路径 /api/student/** 由 AuthInterceptor 强制 STUDENT 角色。
 */
@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {

    private final AttemptService attemptService;

    public StudentExamController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    /**
     * 我的考试列表（含状态与已考成绩）
     */
    @GetMapping
    public List<Map<String, Object>> myExams() {
        return attemptService.myExams();
    }

    /**
     * 开考：入场校验 → 新建/恢复 attempt → 返回题目与剩余时间
     */
    @PostMapping("/{examId}/start")
    public StartExamResponse start(@PathVariable Long examId) {
        return attemptService.start(examId);
    }

    /**
     * 暂存答案（过截止自动交卷）
     */
    @PutMapping("/{examId}/answers")
    public Map<String, String> saveAnswers(@PathVariable Long examId, @RequestBody SaveAnswersDTO dto) {
        attemptService.saveAnswers(examId, dto.answers());
        return Map.of("message", "答案已暂存");
    }

    /**
     * 上报违规事件
     */
    @PostMapping("/{examId}/violations")
    public Map<String, String> reportViolation(@PathVariable Long examId, @RequestBody ViolationDTO dto) {
        attemptService.reportViolation(examId, dto.type());
        return Map.of("message", "违规已记录");
    }

    /**
     * 交卷 → 判分 → 返回成绩
     */
    @PostMapping("/{examId}/submit")
    public Map<String, Object> submit(@PathVariable Long examId) {
        return attemptService.submit(examId);
    }
}
