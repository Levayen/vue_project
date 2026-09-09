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
import com.example.app.entity.QuestionType;
import com.example.app.exception.BusinessException;
import com.example.app.repository.CourseRepository;
import com.example.app.repository.ExamPaperRepository;
import com.example.app.repository.PaperQuestionRepository;
import com.example.app.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * PaperService 单元测试（SPEC-exam-paper）
 * 覆盖手动组卷快照写入、课程归属/重复题目校验、发布与删除保护、随机组卷快照与总分。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaperServiceTest {

    @Mock
    private ExamPaperRepository paperRepository;
    @Mock
    private PaperQuestionRepository paperQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private PaperReferencePort referencePort;

    private PaperService paperService;
    private Course course;
    private Question singleQuestion;
    private Question singleQuestion2;
    private Question judgeQuestion;

    @BeforeEach
    void setUp() {
        // PaperGenerateService 为纯逻辑服务且仅依赖已 mock 的 QuestionRepository，直接用真实实例
        // （Java 26 下 Mockito 无法 mock 具体类）
        PaperGenerateService generateService = new PaperGenerateService(questionRepository);
        paperService = new PaperService(paperRepository, paperQuestionRepository, questionRepository,
                courseRepository, generateService, referencePort, new ObjectMapper());

        course = new Course();
        course.setId(1L);
        course.setCourseName("数据结构");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        singleQuestion = new Question();
        singleQuestion.setId(10L);
        singleQuestion.setCourse(course);
        singleQuestion.setType(QuestionType.SINGLE);
        singleQuestion.setContent("单选题目");
        singleQuestion.setOptions("[{\"key\":\"A\",\"text\":\"甲\"},{\"key\":\"B\",\"text\":\"乙\"}]");
        singleQuestion.setAnswer("A");
        singleQuestion.setScore(new BigDecimal("2"));

        singleQuestion2 = new Question();
        singleQuestion2.setId(12L);
        singleQuestion2.setCourse(course);
        singleQuestion2.setType(QuestionType.SINGLE);
        singleQuestion2.setContent("单选题目二");
        singleQuestion2.setOptions("[{\"key\":\"A\",\"text\":\"甲\"},{\"key\":\"B\",\"text\":\"乙\"}]");
        singleQuestion2.setAnswer("B");
        singleQuestion2.setScore(new BigDecimal("2"));

        judgeQuestion = new Question();
        judgeQuestion.setId(11L);
        judgeQuestion.setCourse(course);
        judgeQuestion.setType(QuestionType.JUDGE);
        judgeQuestion.setContent("判断题目");
        judgeQuestion.setOptions(null);
        judgeQuestion.setAnswer("T");
        judgeQuestion.setScore(new BigDecimal("5"));

        when(questionRepository.findById(10L)).thenReturn(Optional.of(singleQuestion));
        when(questionRepository.findById(11L)).thenReturn(Optional.of(judgeQuestion));
        when(questionRepository.findById(12L)).thenReturn(Optional.of(singleQuestion2));

        when(paperRepository.save(any(ExamPaper.class))).thenAnswer(inv -> {
            ExamPaper p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(100L);
            }
            return p;
        });
        when(paperQuestionRepository.save(any(PaperQuestion.class))).thenAnswer(inv -> {
            PaperQuestion pq = inv.getArgument(0);
            pq.setId(System.nanoTime());
            return pq;
        });
        when(paperQuestionRepository.findByPaper_IdOrderBySeqAsc(anyLong())).thenReturn(List.of());
    }

    private PaperItemDTO item(Long questionId, Integer seq, String score) {
        return new PaperItemDTO(questionId, seq, score == null ? null : new BigDecimal(score),
                null, null, null, null, null, null);
    }

    private PaperDTO manualPayload(Long courseId, List<PaperItemDTO> items) {
        return new PaperDTO(null, "期中试卷", courseId, null, null, null, null, items, null);
    }

    // ---------- 手动组卷 ----------

    @Test
    void createManualWritesSnapshotsAndTotalScore() {
        PaperDTO result = paperService.createManual(manualPayload(1L, List.of(
                item(10L, null, "3"), item(11L, null, "5"))));

        assertEquals(PaperStatus.DRAFT.name(), result.status());
        assertEquals(PaperGenerateType.MANUAL.name(), result.generateType());
        assertEquals(0, new BigDecimal("8").compareTo(result.totalScore()));

        var saved = org.mockito.ArgumentCaptor.forClass(PaperQuestion.class);
        verify(paperQuestionRepository, times(2)).save(saved.capture());
        PaperQuestion first = saved.getAllValues().get(0);
        assertEquals(10L, first.getQuestionId());
        assertEquals("单选题目", first.getContentSnapshot());
        assertEquals("A", first.getAnswerSnapshot());
        assertEquals("SINGLE", first.getTypeSnapshot());
        assertEquals(1, first.getSeq());
        assertNotNull(first.getOptionsSnapshot(), "选择题应写入选项快照");
        PaperQuestion second = saved.getAllValues().get(1);
        assertEquals(2, second.getSeq());
        assertNull(second.getOptionsSnapshot(), "判断题选项快照应为空");
    }

    @Test
    void createManualRejectsQuestionFromOtherCourse() {
        Course other = new Course();
        other.setId(2L);
        other.setCourseName("操作系统");
        singleQuestion.setCourse(other);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperService.createManual(manualPayload(1L, List.of(item(10L, null, "3")))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(paperRepository, never()).save(any());
        singleQuestion.setCourse(course);
    }

    @Test
    void createManualRejectsDuplicateQuestion() {
        assertThrows(BusinessException.class,
                () -> paperService.createManual(manualPayload(1L, List.of(
                        item(10L, 1, "3"), item(10L, 2, "3")))));
    }

    @Test
    void createManualRejectsDuplicateSeq() {
        assertThrows(BusinessException.class,
                () -> paperService.createManual(manualPayload(1L, List.of(
                        item(10L, 1, "3"), item(11L, 1, "5")))));
    }

    @Test
    void createManualRejectsEmptyItems() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperService.createManual(manualPayload(1L, List.of())));
        assertTrue(ex.getMessage().contains("至少"));
    }

    @Test
    void createManualRejectsZeroScore() {
        assertThrows(BusinessException.class,
                () -> paperService.createManual(manualPayload(1L, List.of(item(10L, 1, "0")))));
    }

    // ---------- 编辑与发布 ----------

    @Test
    void updatePublishedPaperConflict() {
        ExamPaper published = new ExamPaper();
        published.setId(100L);
        published.setStatus(PaperStatus.PUBLISHED);
        when(paperRepository.findById(100L)).thenReturn(Optional.of(published));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paperService.update(100L, manualPayload(1L, List.of(item(10L, null, null)))));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void updateDraftReplacesItems() {
        ExamPaper draft = new ExamPaper();
        draft.setId(100L);
        draft.setName("旧名称");
        draft.setCourse(course);
        draft.setGenerateType(PaperGenerateType.MANUAL);
        draft.setStatus(PaperStatus.DRAFT);
        when(paperRepository.findById(100L)).thenReturn(Optional.of(draft));

        PaperDTO result = paperService.update(100L, manualPayload(1L, List.of(item(11L, null, null))));
        verify(paperQuestionRepository).deleteByPaper_Id(100L);
        assertEquals("期中试卷", result.name());
    }

    @Test
    void publishDraftSuccess() {
        ExamPaper draft = new ExamPaper();
        draft.setId(100L);
        draft.setCourse(course);
        draft.setGenerateType(PaperGenerateType.MANUAL);
        draft.setStatus(PaperStatus.DRAFT);
        when(paperRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(paperQuestionRepository.findByPaper_IdOrderBySeqAsc(100L))
                .thenReturn(List.of(new PaperQuestion()));

        PaperDTO result = paperService.publish(100L);
        assertEquals(PaperStatus.PUBLISHED.name(), result.status());
    }

    @Test
    void publishEmptyPaperRejected() {
        ExamPaper draft = new ExamPaper();
        draft.setId(100L);
        draft.setStatus(PaperStatus.DRAFT);
        when(paperRepository.findById(100L)).thenReturn(Optional.of(draft));

        BusinessException ex = assertThrows(BusinessException.class, () -> paperService.publish(100L));
        assertTrue(ex.getMessage().contains("题目"));
    }

    @Test
    void publishAlreadyPublishedConflict() {
        ExamPaper published = new ExamPaper();
        published.setId(100L);
        published.setStatus(PaperStatus.PUBLISHED);
        when(paperRepository.findById(100L)).thenReturn(Optional.of(published));

        BusinessException ex = assertThrows(BusinessException.class, () -> paperService.publish(100L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ---------- 删除保护 ----------

    @Test
    void deleteReferencedByExamConflict() {
        ExamPaper paper = new ExamPaper();
        paper.setId(100L);
        when(paperRepository.findById(100L)).thenReturn(Optional.of(paper));
        when(referencePort.isReferencedByExam(100L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> paperService.delete(100L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(paperRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteUnreferencedPaperOk() {
        ExamPaper paper = new ExamPaper();
        paper.setId(100L);
        when(paperRepository.findById(100L)).thenReturn(Optional.of(paper));
        when(referencePort.isReferencedByExam(100L)).thenReturn(false);

        paperService.delete(100L);
        verify(paperQuestionRepository).deleteByPaper_Id(100L);
        verify(paperRepository).deleteById(100L);
    }

    // ---------- 随机组卷 ----------

    @Test
    void createRandomWritesSnapshotsAndTotalScore() {
        when(questionRepository.findByCourse_IdAndTypeOrderByIdAsc(1L, QuestionType.SINGLE))
                .thenReturn(List.of(singleQuestion, singleQuestion2));
        when(questionRepository.findByCourse_IdAndTypeOrderByIdAsc(1L, QuestionType.JUDGE))
                .thenReturn(List.of(judgeQuestion));

        RandomPaperRuleDTO payload = new RandomPaperRuleDTO("随机卷", 1L, List.of(
                new DrawRule("SINGLE", null, 2, new BigDecimal("2")),
                new DrawRule("JUDGE", null, 1, new BigDecimal("4"))));

        PaperDTO result = paperService.createRandom(payload);

        assertEquals(PaperGenerateType.RANDOM.name(), result.generateType());
        // 2*2 + 1*4 = 8
        assertEquals(0, new BigDecimal("8").compareTo(result.totalScore()));
        verify(paperQuestionRepository, times(3)).save(any(PaperQuestion.class));
    }

    @Test
    void createRandomRejectsDuplicateRuleType() {
        RandomPaperRuleDTO payload = new RandomPaperRuleDTO("随机卷", 1L, List.of(
                new DrawRule("SINGLE", null, 2, new BigDecimal("2")),
                new DrawRule("single", null, 3, new BigDecimal("2"))));

        BusinessException ex = assertThrows(BusinessException.class, () -> paperService.createRandom(payload));
        assertTrue(ex.getMessage().contains("同一题型"));
        verify(paperRepository, never()).save(any());
    }

    @Test
    void createRandomRejectsEmptyRules() {
        assertThrows(BusinessException.class,
                () -> paperService.createRandom(new RandomPaperRuleDTO("随机卷", 1L, List.of())));
    }

    // ---------- 详情快照读取 ----------

    @Test
    void getReturnsItemsWithSnapshotAnswer() {
        ExamPaper paper = new ExamPaper();
        paper.setId(100L);
        paper.setName("期中试卷");
        paper.setCourse(course);
        paper.setGenerateType(PaperGenerateType.MANUAL);
        paper.setStatus(PaperStatus.DRAFT);
        paper.setTotalScore(new BigDecimal("3"));
        when(paperRepository.findById(100L)).thenReturn(Optional.of(paper));

        PaperQuestion pq = new PaperQuestion();
        pq.setQuestionId(10L);
        pq.setSeq(1);
        pq.setScore(new BigDecimal("3"));
        pq.setContentSnapshot("单选题目");
        pq.setOptionsSnapshot("[{\"key\":\"A\",\"text\":\"甲\"},{\"key\":\"B\",\"text\":\"乙\"}]");
        pq.setAnswerSnapshot("A");
        pq.setTypeSnapshot("SINGLE");
        when(paperQuestionRepository.findByPaper_IdOrderBySeqAsc(100L)).thenReturn(List.of(pq));

        PaperDTO result = paperService.get(100L);
        assertEquals(1, result.items().size());
        PaperItemDTO dto = result.items().get(0);
        assertEquals("A", dto.answer());
        assertEquals(2, dto.options().size());
        assertEquals(QuestionOption.class, dto.options().get(0).getClass());
    }
}
