package com.example.app.controller;

import com.example.app.dto.PendingAttemptDTO;
import com.example.app.dto.PendingExamDTO;
import com.example.app.dto.ReviewAttemptDTO;
import com.example.app.dto.ScoreRequest;
import com.example.app.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师人工阅卷接口（M7：主观题简答/论述）
 * 路径 /api/teacher/** 由 AuthInterceptor 限定 TEACHER/ADMIN。
 */
@RestController
@RequestMapping("/api/teacher/reviews")
public class TeacherReviewController {

    private final ReviewService reviewService;

    public TeacherReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 待阅考试列表（有待评主观题答卷的考试）
     */
    @GetMapping("/pending")
    public List<PendingExamDTO> pendingExams() {
        return reviewService.pendingExams();
    }

    /**
     * 某场考试的待评学生答卷列表
     */
    @GetMapping("/exams/{examId}/pending")
    public List<PendingAttemptDTO> pendingAttempts(@PathVariable Long examId) {
        return reviewService.pendingAttempts(examId);
    }

    /**
     * 一份答卷的主观题评分详情
     */
    @GetMapping("/attempts/{attemptId}")
    public ReviewAttemptDTO reviewAttempt(@PathVariable Long attemptId) {
        return reviewService.reviewAttempt(attemptId);
    }

    /**
     * 提交单道主观题评分（全部评完自动定稿成绩）
     */
    @PostMapping("/answers/{answerId}/score")
    public Map<String, Object> scoreAnswer(@PathVariable Long answerId,
                                           @RequestBody ScoreRequest request) {
        return reviewService.scoreAnswer(answerId, request);
    }
}
