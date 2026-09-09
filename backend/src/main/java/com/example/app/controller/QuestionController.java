package com.example.app.controller;

import com.example.app.dto.QuestionDTO;
import com.example.app.dto.QuestionQuery;
import com.example.app.entity.UserRole;
import com.example.app.security.RequireRoles;
import com.example.app.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 题库管理接口（SPEC-question-bank）
 * 教师与管理员可访问，学生角色由 @RequireRoles 拦截返回 403。
 */
@RestController
@RequestMapping("/api/questions")
@RequireRoles({UserRole.TEACHER, UserRole.ADMIN})
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 分页查询题目
     */
    @GetMapping
    public Page<QuestionDTO> page(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return questionService.page(new QuestionQuery(courseId, type, difficulty, keyword, page, size));
    }

    /**
     * 题目详情
     */
    @GetMapping("/{id}")
    public QuestionDTO get(@PathVariable Long id) {
        return questionService.get(id);
    }

    /**
     * 新建题目
     */
    @PostMapping
    public QuestionDTO create(@Valid @RequestBody QuestionDTO dto) {
        return questionService.create(dto);
    }

    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    public QuestionDTO update(@PathVariable Long id, @Valid @RequestBody QuestionDTO dto) {
        return questionService.update(id, dto);
    }

    /**
     * 删除题目（被试卷引用时 409）
     */
    @DeleteMapping("/{id}")
    public java.util.Map<String, String> delete(@PathVariable Long id) {
        questionService.delete(id);
        return java.util.Map.of("message", "题目已删除");
    }
}
