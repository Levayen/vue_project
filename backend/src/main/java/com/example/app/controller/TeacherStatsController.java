package com.example.app.controller;

import com.example.app.dto.ExamStatsDTO;
import com.example.app.dto.TeacherRecordDTO;
import com.example.app.service.ExamStatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师成绩统计接口（SPEC-results）
 * 路径 /api/teacher/** 由 AuthInterceptor 限定 TEACHER/ADMIN。
 */
@RestController
@RequestMapping("/api/teacher/exams")
public class TeacherStatsController {

    private final ExamStatsService examStatsService;

    public TeacherStatsController(ExamStatsService examStatsService) {
        this.examStatsService = examStatsService;
    }

    /**
     * 成绩列表（可按班级筛选）
     */
    @GetMapping("/{examId}/records")
    public List<TeacherRecordDTO> records(@PathVariable Long examId,
                                          @RequestParam(required = false) Long classId) {
        return examStatsService.records(examId, classId);
    }

    /**
     * 统计指标（均分/及格率/分数段；无人参考返回零值）
     */
    @GetMapping("/{examId}/stats")
    public ExamStatsDTO stats(@PathVariable Long examId,
                              @RequestParam(required = false) Long classId) {
        return examStatsService.stats(examId, classId);
    }
}
