package com.example.app.service;

import com.example.app.dto.DrawRule;
import com.example.app.dto.PaperDTO;
import com.example.app.dto.PaperItemDTO;
import com.example.app.dto.QuestionOption;
import com.example.app.dto.RandomPaperRuleDTO;
import com.example.app.entity.Course;
import com.example.app.entity.ExamPaper;
import com.example.app.entity.PaperGenerateType;
import com.example.app.entity.PaperQuestion;
import com.example.app.entity.PaperStatus;
import com.example.app.entity.Question;
import com.example.app.exception.BusinessException;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.ExamPaperRepository;
import com.example.app.repository.PaperQuestionRepository;
import com.example.app.repository.QuestionRepository;
import com.example.app.security.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 试卷服务（SPEC-exam-paper）
 * 手动组卷 / 随机组卷 / 草稿编辑 / 发布 / 删除。
 * 组卷时把题干、选项、答案、题型写入快照表；业务校验全部在本层。
 */
@Service
public class PaperService {

    private final ExamPaperRepository paperRepository;
    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final PaperGenerateService generateService;
    private final PaperReferencePort referencePort;
    private final ObjectMapper objectMapper;

    public PaperService(ExamPaperRepository paperRepository,
                        PaperQuestionRepository paperQuestionRepository,
                        QuestionRepository questionRepository,
                        CourseRepository courseRepository,
                        PaperGenerateService generateService,
                        PaperReferencePort referencePort,
                        ObjectMapper objectMapper) {
        this.paperRepository = paperRepository;
        this.paperQuestionRepository = paperQuestionRepository;
        this.questionRepository = questionRepository;
        this.courseRepository = courseRepository;
        this.generateService = generateService;
        this.referencePort = referencePort;
        this.objectMapper = objectMapper;
    }

    /**
     * 试卷分页（列表不含题目明细）
     */
    public Page<PaperDTO> page(Long courseId, Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size <= 0 ? 10 : Math.min(size, 100);
        PageRequest pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"));
        Page<ExamPaper> result = courseId == null
                ? paperRepository.findAll(pageable)
                : paperRepository.findByCourse_Id(courseId, pageable);
        return result.map(paper -> toDTO(paper, false));
    }

    /**
     * 试卷详情（含快照题目与答案，教师视图）
     */
    public PaperDTO get(Long id) {
        return toDTO(requirePaper(id), true);
    }

    /**
     * 手动组卷：从题库勾选题目，写入快照
     */
    @Transactional
    public PaperDTO createManual(PaperDTO dto) {
        Course course = requireCourse(dto.courseId());
        List<PaperItemDTO> items = requireItems(dto.items());

        ExamPaper paper = new ExamPaper();
        paper.setName(requireName(dto.name()));
        paper.setCourse(course);
        paper.setGenerateType(PaperGenerateType.MANUAL);
        paper.setStatus(PaperStatus.DRAFT);
        paper.setCreateBy(UserContext.currentUserId());
        savePaperWithItems(paper, items);
        return toDTO(paper, true);
    }

    /**
     * 随机组卷：按规则抽题后写入快照；任一规则题量不足则整体失败（事务回滚，无脏数据）
     */
    @Transactional
    public PaperDTO createRandom(RandomPaperRuleDTO dto) {
        Course course = requireCourse(dto.courseId());
        List<DrawRule> rules = requireRules(dto.rules());
        validateRuleTypes(rules);

        ExamPaper paper = new ExamPaper();
        paper.setName(requireName(dto.name()));
        paper.setCourse(course);
        paper.setGenerateType(PaperGenerateType.RANDOM);
        paper.setStatus(PaperStatus.DRAFT);
        paper.setCreateBy(UserContext.currentUserId());

        List<PaperItemDTO> items = new ArrayList<>();
        int seq = 1;
        for (DrawRule rule : rules) {
            generateService.validateScorePerQuestion(rule.scorePerQuestion());
            List<Question> drawn = generateService.draw(course.getId(), rule);
            for (Question q : drawn) {
                items.add(new PaperItemDTO(q.getId(), seq++, rule.scorePerQuestion(),
                        q.getType().name(), q.getContent(), parseOptionsJson(q.getOptions()), q.getAnswer(),
                        q.getAnalysis(), q.getReferenceAnswer()));
            }
        }
        savePaperWithItems(paper, items);
        return toDTO(paper, true);
    }

    /**
     * 编辑草稿（整卷替换题目）；已发布试卷拒绝修改
     */
    @Transactional
    public PaperDTO update(Long id, PaperDTO dto) {
        ExamPaper paper = requirePaper(id);
        if (paper.getStatus() != PaperStatus.DRAFT) {
            throw BusinessException.conflict("已发布试卷不可编辑");
        }
        Course course = requireCourse(dto.courseId());
        List<PaperItemDTO> items = requireItems(dto.items());

        paper.setName(requireName(dto.name()));
        paper.setCourse(course);
        paperQuestionRepository.deleteByPaper_Id(id);
        savePaperWithItems(paper, items);
        return toDTO(paper, true);
    }

    /**
     * 删除试卷；已被考试引用时拒绝（409）
     */
    @Transactional
    public void delete(Long id) {
        requirePaper(id);
        if (referencePort.isReferencedByExam(id)) {
            throw BusinessException.conflict("试卷已被考试引用，无法删除");
        }
        paperQuestionRepository.deleteByPaper_Id(id);
        paperRepository.deleteById(id);
    }

    /**
     * 发布：DRAFT → PUBLISHED，发布后不可改；空卷不可发布
     */
    @Transactional
    public PaperDTO publish(Long id) {
        ExamPaper paper = requirePaper(id);
        if (paper.getStatus() == PaperStatus.PUBLISHED) {
            throw BusinessException.conflict("试卷已是发布状态");
        }
        if (paperQuestionRepository.findByPaper_IdOrderBySeqAsc(id).isEmpty()) {
            throw BusinessException.badRequest("试卷没有任何题目，无法发布");
        }
        paper.setStatus(PaperStatus.PUBLISHED);
        paperRepository.save(paper);
        return toDTO(paper, true);
    }

    // ==================== 内部方法 ====================

    private ExamPaper requirePaper(Long id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("试卷不存在"));
    }

    private Course requireCourse(Long courseId) {
        if (courseId == null) {
            throw BusinessException.badRequest("请选择归属课程");
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> BusinessException.badRequest("归属课程不存在"));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw BusinessException.badRequest("试卷名称不能为空");
        }
        return name.trim();
    }

    private List<PaperItemDTO> requireItems(List<PaperItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw BusinessException.badRequest("试卷至少需要一道题目");
        }
        return items;
    }

    private List<DrawRule> requireRules(List<DrawRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw BusinessException.badRequest("请至少设置一条抽题规则");
        }
        return rules;
    }

    /**
     * 同一题型只允许一条规则，避免重复抽中同一题
     */
    private void validateRuleTypes(List<DrawRule> rules) {
        Set<String> seen = new HashSet<>();
        for (DrawRule rule : rules) {
            String type = rule.type() == null ? "" : rule.type().trim().toUpperCase();
            if (!seen.add(type)) {
                throw BusinessException.badRequest("同一题型只能设置一条抽题规则：" + type);
            }
        }
    }

    /**
     * 校验题目条目并写入快照；汇总总分写入试卷。
     * 先完整校验全部条目，再统一落库，保证校验失败时不产生任何数据。
     */
    private void savePaperWithItems(ExamPaper paper, List<PaperItemDTO> items) {
        List<PaperQuestion> snapshots = buildSnapshots(paper, items);

        paperRepository.save(paper);
        snapshots.forEach(paperQuestionRepository::save);

        BigDecimal total = snapshots.stream()
                .map(PaperQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(total);
        paperRepository.save(paper);
    }

    /**
     * 校验并构建快照行（不落库）：课程归属、重复题目、顺序唯一、分值范围
     */
    private List<PaperQuestion> buildSnapshots(ExamPaper paper, List<PaperItemDTO> items) {
        Set<Long> questionIds = new HashSet<>();
        Set<Integer> seqs = new HashSet<>();
        List<PaperQuestion> snapshots = new ArrayList<>();
        int autoSeq = 1;

        for (PaperItemDTO item : items) {
            if (item.questionId() == null) {
                throw BusinessException.badRequest("题目条目缺少题目 id");
            }
            Question question = questionRepository.findById(item.questionId())
                    .orElseThrow(() -> BusinessException.badRequest("题目不存在：" + item.questionId()));
            if (!question.getCourse().getId().equals(paper.getCourse().getId())) {
                throw BusinessException.badRequest(
                        "题目「" + question.getContent().substring(0, Math.min(10, question.getContent().length()))
                                + "…」不属于该课程");
            }
            if (!questionIds.add(question.getId())) {
                throw BusinessException.badRequest("同一题目不能重复加入试卷");
            }

            int seq = item.seq() == null ? autoSeq : item.seq();
            if (seq < 1) {
                throw BusinessException.badRequest("题目顺序必须为正整数");
            }
            if (!seqs.add(seq)) {
                throw BusinessException.badRequest("题目顺序重复：" + seq);
            }

            BigDecimal score = item.score() == null ? question.getScore() : item.score();
            if (score == null || score.compareTo(BigDecimal.ZERO) <= 0 || score.compareTo(new BigDecimal("100")) > 0) {
                throw BusinessException.badRequest("分值必须大于 0 且不超过 100");
            }

            PaperQuestion pq = new PaperQuestion();
            pq.setPaper(paper);
            pq.setQuestionId(question.getId());
            pq.setSeq(seq);
            pq.setScore(score);
            pq.setContentSnapshot(question.getContent());
            pq.setOptionsSnapshot(question.getOptions());
            pq.setAnswerSnapshot(question.getAnswer());
            pq.setTypeSnapshot(question.getType().name());
            pq.setAnalysisSnapshot(question.getAnalysis());
            snapshots.add(pq);
            autoSeq++;
        }
        return snapshots;
    }

    private PaperDTO toDTO(ExamPaper paper, boolean withItems) {
        List<PaperItemDTO> items = null;
        if (withItems) {
            items = paperQuestionRepository.findByPaper_IdOrderBySeqAsc(paper.getId()).stream()
                    .map(this::toItemDTO)
                    .toList();
        }
        return new PaperDTO(
                paper.getId(),
                paper.getName(),
                paper.getCourse().getId(),
                paper.getCourse().getCourseName(),
                paper.getTotalScore(),
                paper.getGenerateType().name(),
                paper.getStatus().name(),
                items,
                paper.getCreateTime()
        );
    }

    private PaperItemDTO toItemDTO(PaperQuestion pq) {
        return new PaperItemDTO(
                pq.getQuestionId(),
                pq.getSeq(),
                pq.getScore(),
                pq.getTypeSnapshot(),
                pq.getContentSnapshot(),
                parseOptionsJson(pq.getOptionsSnapshot()),
                pq.getAnswerSnapshot(),
                pq.getAnalysisSnapshot(),
                pq.getReferenceAnswerSnapshot()
        );
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
}
