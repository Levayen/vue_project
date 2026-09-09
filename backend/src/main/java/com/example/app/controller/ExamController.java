package com.example.app.controller;

import com.example.app.dto.ExamDTO;
import com.example.app.dto.MonitoringDTO;
import com.example.app.entity.UserRole;
import com.example.app.security.RequireRoles;
import com.example.app.service.ExamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考试管理接口（SPEC-exam-session + M6 监考，教师/管理员）
 * 发布考试、考试列表、监考视图、强制收卷。
 */
@RestController
@RequestMapping("/api/exams")
@RequireRoles({UserRole.TEACHER, UserRole.ADMIN})
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * 发布考试
     */
    @PostMapping
    public ExamDTO publish(@RequestBody ExamDTO dto) {
        return examService.publish(dto);
    }

    /**
     * 考试列表（可按课程筛选）
     */
    @GetMapping
    public List<ExamDTO> list(@RequestParam(required = false) Long courseId) {
        return examService.list(courseId);
    }

    /**
     * 监考视图：参考学生、尝试状态、违规次数
     */
    @GetMapping("/{id}/monitoring")
    public MonitoringDTO monitoring(@PathVariable Long id) {
        return examService.monitoring(id);
    }

    /**
     * 强制收卷（M6）：对指定学生或全体进行中考生强制交卷判分
     * body: { "studentId": 123 }  不传则收卷全体进行中考生
     */
    @PostMapping("/{id}/force-submit")
    public Map<String, Object> forceSubmit(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, Object> body) {
        Long studentId = null;
        if (body != null && body.get("studentId") != null) {
            studentId = Long.valueOf(body.get("studentId").toString());
        }
        int count = examService.forceSubmit(id, studentId);
        return Map.of("forcedCount", count);
    }
}
