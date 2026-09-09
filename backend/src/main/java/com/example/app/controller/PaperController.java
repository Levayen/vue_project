package com.example.app.controller;

import com.example.app.dto.PaperDTO;
import com.example.app.dto.RandomPaperRuleDTO;
import com.example.app.entity.UserRole;
import com.example.app.security.RequireRoles;
import com.example.app.service.PaperService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 试卷管理接口（SPEC-exam-paper）
 * 教师与管理员可访问，学生角色由 @RequireRoles 拦截返回 403。
 */
@RestController
@RequestMapping("/api/papers")
@RequireRoles({UserRole.TEACHER, UserRole.ADMIN})
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    /**
     * 试卷分页查询
     */
    @GetMapping
    public Page<PaperDTO> page(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return paperService.page(courseId, page, size);
    }

    /**
     * 试卷详情（含快照题目，教师视图带答案）
     */
    @GetMapping("/{id}")
    public PaperDTO get(@PathVariable Long id) {
        return paperService.get(id);
    }

    /**
     * 手动组卷
     */
    @PostMapping("/manual")
    public PaperDTO createManual(@RequestBody PaperDTO dto) {
        return paperService.createManual(dto);
    }

    /**
     * 随机组卷（题量不足返回 400 并说明缺口）
     */
    @PostMapping("/random")
    public PaperDTO createRandom(@RequestBody RandomPaperRuleDTO dto) {
        return paperService.createRandom(dto);
    }

    /**
     * 编辑草稿（仅 DRAFT 可改，发布后 409）
     */
    @PutMapping("/{id}")
    public PaperDTO update(@PathVariable Long id, @RequestBody PaperDTO dto) {
        return paperService.update(id, dto);
    }

    /**
     * 删除试卷（被考试引用时 409）
     */
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        paperService.delete(id);
        return Map.of("message", "试卷已删除");
    }

    /**
     * 发布试卷：DRAFT → PUBLISHED，发布后不可改
     */
    @PostMapping("/{id}/publish")
    public PaperDTO publish(@PathVariable Long id) {
        return paperService.publish(id);
    }
}
