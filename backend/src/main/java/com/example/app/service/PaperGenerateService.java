package com.example.app.service;

import com.example.app.dto.DrawRule;
import com.example.app.entity.Question;
import com.example.app.entity.QuestionType;
import com.example.app.exception.BusinessException;
import com.example.app.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 随机抽题服务（SPEC-exam-paper）
 * 纯函数式抽题算法：按题型（+可选难度）从课程题库随机抽取指定数量题目。
 * 不依赖 HTTP、不落库，便于单测；题量不足时抛 400 并说明缺口，由调用方保证整体失败。
 */
@Service
public class PaperGenerateService {

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final QuestionRepository questionRepository;

    public PaperGenerateService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 按规则从课程题库随机抽题
     *
     * @param courseId 课程 id
     * @param rule     抽题规则（题型/难度/数量）
     * @return 抽中的题目列表，不重复
     */
    public List<Question> draw(Long courseId, DrawRule rule) {
        QuestionType type = parseType(rule.type());
        if (rule.count() == null || rule.count() < 1) {
            throw BusinessException.badRequest("抽题数量必须大于 0");
        }

        List<Question> pool = questionRepository.findByCourse_IdAndTypeOrderByIdAsc(courseId, type);
        List<Question> filtered = new ArrayList<>(rule.difficulty() == null
                ? pool
                : pool.stream().filter(q -> q.getDifficulty().equals(rule.difficulty())).toList());

        if (filtered.size() < rule.count()) {
            throw BusinessException.badRequest(
                    ("题型 %s%s 需要 %d 题，题库仅有 %d 题，请先补充题库或调整抽题规则")
                            .formatted(type, rule.difficulty() == null ? "" : "（难度" + rule.difficulty() + "）",
                                    rule.count(), filtered.size()));
        }

        Collections.shuffle(filtered);
        return new ArrayList<>(filtered.subList(0, rule.count()));
    }

    /**
     * 校验每题分值（与题库分值上限一致）
     */
    public void validateScorePerQuestion(BigDecimal scorePerQuestion) {
        if (scorePerQuestion == null || scorePerQuestion.compareTo(BigDecimal.ZERO) <= 0
                || scorePerQuestion.compareTo(MAX_SCORE) > 0) {
            throw BusinessException.badRequest("每题分值必须大于 0 且不超过 100");
        }
    }

    private QuestionType parseType(String type) {
        try {
            return QuestionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw BusinessException.badRequest("非法题型：" + type);
        }
    }
}
