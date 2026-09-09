package com.example.app.service;

import com.example.app.dto.QuestionDTO;
import com.example.app.dto.QuestionOption;
import com.example.app.dto.QuestionQuery;
import com.example.app.entity.Course;
import com.example.app.entity.Question;
import com.example.app.entity.QuestionType;
import com.example.app.exception.BusinessException;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库服务（SPEC-question-bank）
 * 题目的分页查询、增删改；按题型校验选项与答案、规范化答案编码；
 * 删除时校验试卷引用。业务校验全部在本层，Controller 只做参数绑定。
 */
@Service
public class QuestionService {

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final QuestionReferencePort referencePort;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepository questionRepository,
                           CourseRepository courseRepository,
                           QuestionReferencePort referencePort,
                           ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.courseRepository = courseRepository;
        this.referencePort = referencePort;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询（课程/题型/难度/关键词）
     */
    public Page<QuestionDTO> page(QuestionQuery query) {
        int page = query.page() == null || query.page() < 0 ? 0 : query.page();
        int size = query.size() == null || query.size() <= 0 ? 10 : Math.min(query.size(), 100);

        Specification<Question> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.courseId() != null) {
                predicates.add(cb.equal(root.get("course").get("id"), query.courseId()));
            }
            if (query.type() != null && !query.type().isBlank()) {
                predicates.add(cb.equal(root.get("type"), parseType(query.type())));
            }
            if (query.difficulty() != null) {
                predicates.add(cb.equal(root.get("difficulty"), query.difficulty()));
            }
            if (query.keyword() != null && !query.keyword().isBlank()) {
                predicates.add(cb.like(root.get("content"), "%" + query.keyword().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return questionRepository
                .findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toDTO);
    }

    /**
     * 题目详情
     */
    public QuestionDTO get(Long id) {
        return toDTO(requireQuestion(id));
    }

    /**
     * 新建题目
     */
    @Transactional
    public QuestionDTO create(QuestionDTO dto) {
        Question question = new Question();
        apply(question, dto);
        return toDTO(questionRepository.save(question));
    }

    /**
     * 更新题目
     */
    @Transactional
    public QuestionDTO update(Long id, QuestionDTO dto) {
        Question question = requireQuestion(id);
        apply(question, dto);
        return toDTO(questionRepository.save(question));
    }

    /**
     * 删除题目；已被试卷引用时拒绝（409）
     */
    @Transactional
    public void delete(Long id) {
        requireQuestion(id);
        if (referencePort.isReferencedByPaper(id)) {
            throw BusinessException.conflict("题目已被试卷引用，请先从相关试卷移除后再删除");
        }
        questionRepository.deleteById(id);
    }

    // ==================== 内部方法 ====================

    private Question requireQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("题目不存在"));
    }

    /**
     * 校验并把 DTO 字段写入实体（含答案编码规范化）
     */
    private void apply(Question question, QuestionDTO dto) {
        QuestionType type = parseType(dto.type());

        Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() -> BusinessException.badRequest("所属课程不存在"));
        question.setCourse(course);
        question.setType(type);
        question.setContent(dto.content().trim());

        BigDecimal score = dto.score() == null ? new BigDecimal("5") : dto.score();
        if (score.compareTo(BigDecimal.ZERO) <= 0 || score.compareTo(MAX_SCORE) > 0) {
            throw BusinessException.badRequest("分值必须大于 0 且不超过 100");
        }
        question.setScore(score);

        int difficulty = dto.difficulty() == null ? 1 : dto.difficulty();
        if (difficulty < 1 || difficulty > 3) {
            throw BusinessException.badRequest("难度必须为 1（易）/2（中）/3（难）");
        }
        question.setDifficulty(difficulty);
        question.setAnalysis(dto.analysis() == null || dto.analysis().isBlank() ? null : dto.analysis().trim());

        List<QuestionOption> options = dto.options() == null ? List.of() : dto.options();
        switch (type) {
            case SINGLE -> {
                Set<String> keys = validateOptions(options, 2);
                String answer = normalizeLetter(dto.answer(), false);
                if (answer.length() != 1 || !keys.contains(answer)) {
                    throw BusinessException.badRequest("单选题答案必须是选项之一");
                }
                question.setOptions(toOptionsJson(options));
                question.setAnswer(answer);
                question.setReferenceAnswer(null);
            }
            case MULTI -> {
                Set<String> keys = validateOptions(options, 2);
                String answer = normalizeLetter(dto.answer(), true);
                if (answer.length() < 2) {
                    throw BusinessException.badRequest("多选题答案至少包含 2 个选项");
                }
                for (char c : answer.toCharArray()) {
                    if (!keys.contains(String.valueOf(c))) {
                        throw BusinessException.badRequest("多选题答案包含不存在的选项：" + c);
                    }
                }
                question.setOptions(toOptionsJson(options));
                question.setAnswer(answer);
                question.setReferenceAnswer(null);
            }
            case JUDGE -> {
                String answer = dto.answer() == null ? "" : dto.answer().trim().toUpperCase();
                if (!answer.equals("T") && !answer.equals("F")) {
                    throw BusinessException.badRequest("判断题答案必须为 T（正确）或 F（错误）");
                }
                question.setOptions(null);
                question.setAnswer(answer);
                question.setReferenceAnswer(null);
            }
            case FILL -> {
                String answer = normalizeFillAnswer(dto.answer());
                if (answer.isEmpty()) {
                    throw BusinessException.badRequest("填空题答案不能为空");
                }
                question.setOptions(null);
                question.setAnswer(answer);
                question.setReferenceAnswer(null);
            }
            case SHORT_ANSWER, ESSAY -> {
                // 主观题（M7）：无选项、无标准答案编码；answer 字段承载参考答案（选填）
                question.setOptions(null);
                question.setAnswer("");
                String ref = dto.answer() == null || dto.answer().isBlank()
                        ? null : dto.answer().trim();
                question.setReferenceAnswer(ref);
            }
        }
    }

    /**
     * 校验选项列表：至少 min 项，key 非空不重复，text 非空；返回合法 key 集合
     */
    private Set<String> validateOptions(List<QuestionOption> options, int min) {
        if (options == null || options.size() < min) {
            throw BusinessException.badRequest("选择题至少需要 " + min + " 个选项");
        }
        Set<String> keys = new HashSet<>();
        for (QuestionOption option : options) {
            if (option == null || option.key() == null || option.key().isBlank()
                    || option.text() == null || option.text().isBlank()) {
                throw BusinessException.badRequest("选项标识和内容均不能为空");
            }
            String key = option.key().trim().toUpperCase();
            if (!keys.add(key)) {
                throw BusinessException.badRequest("选项标识重复：" + key);
            }
        }
        return keys;
    }

    /**
     * 单选/多选答案规范化：去空白、大写、去重；multi=true 时按字母排序
     */
    private String normalizeLetter(String raw, boolean multi) {
        if (raw == null) {
            return "";
        }
        Set<String> chars = new HashSet<>();
        for (char c : raw.trim().toUpperCase().toCharArray()) {
            if (!Character.isWhitespace(c) && c != ',' && c != '，') {
                chars.add(String.valueOf(c));
            }
        }
        return chars.stream().sorted().collect(Collectors.joining());
    }

    /**
     * 填空答案规范化：按 || 拆分、各段 trim、忽略空段，重新用 || 拼接
     */
    private String normalizeFillAnswer(String raw) {
        if (raw == null) {
            return "";
        }
        return java.util.Arrays.stream(raw.split("\\|\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("||"));
    }

    private String toOptionsJson(List<QuestionOption> options) {
        try {
            List<QuestionOption> normalized = options.stream()
                    .map(o -> new QuestionOption(o.key().trim().toUpperCase(), o.text().trim()))
                    .toList();
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw BusinessException.badRequest("选项数据格式错误");
        }
    }

    private List<QuestionOption> parseOptionsJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOption>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private QuestionDTO toDTO(Question question) {
        // 主观题无答案编码，answer 字段出参承载参考答案（前端回显/展示）
        String answer = question.getType().isSubjective()
                ? question.getReferenceAnswer()
                : question.getAnswer();
        return new QuestionDTO(
                question.getId(),
                question.getCourse().getId(),
                question.getCourse().getCourseName(),
                question.getType().name(),
                question.getContent(),
                parseOptionsJson(question.getOptions()),
                answer,
                question.getScore(),
                question.getDifficulty(),
                question.getAnalysis()
        );
    }

    private QuestionType parseType(String type) {
        try {
            return QuestionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("非法题型：" + type);
        }
    }
}
