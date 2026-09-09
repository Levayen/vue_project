package com.example.app.service;

import com.example.app.dto.ExamStatsDTO;
import com.example.app.dto.ScoreBucketDTO;
import com.example.app.dto.TeacherRecordDTO;
import com.example.app.entity.ExamAttempt;
import com.example.app.entity.ExamRecord;
import com.example.app.entity.Student;
import com.example.app.repository.ExamAttemptRepository;
import com.example.app.repository.ExamRecordRepository;
import com.example.app.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师成绩统计服务（SPEC-results）
 * 成绩列表（可按班级筛选）与统计指标（参考人数/均分/最高最低/及格率/分数段分布）。
 * 统计为纯计算方法（summarize），口径常量化，便于单测。
 */
@Service
public class ExamStatsService {

    /** 及格线 */
    public static final BigDecimal PASS_SCORE = ResultService.PASS_SCORE;

    /** 分数段边界（上界开区间，最后一段含 100）：[0-60)/[60-70)/[70-80)/[80-90)/[90-100] */
    public static final List<ScoreBucketDTO> BUCKET_LABELS = List.of(
            new ScoreBucketDTO("[0-60)", 0),
            new ScoreBucketDTO("[60-70)", 0),
            new ScoreBucketDTO("[70-80)", 0),
            new ScoreBucketDTO("[80-90)", 0),
            new ScoreBucketDTO("[90-100]", 0)
    );

    private final ExamRecordRepository examRecordRepository;
    private final StudentRepository studentRepository;
    private final ExamAttemptRepository attemptRepository;

    public ExamStatsService(ExamRecordRepository examRecordRepository,
                            StudentRepository studentRepository,
                            ExamAttemptRepository attemptRepository) {
        this.examRecordRepository = examRecordRepository;
        this.studentRepository = studentRepository;
        this.attemptRepository = attemptRepository;
    }

    /**
     * 一场考试的成绩列表，可按班级筛选
     */
    @Transactional(readOnly = true)
    public List<TeacherRecordDTO> records(Long examId, Long classId) {
        List<ExamRecord> records = filteredRecords(examId, classId);
        Map<Long, Integer> violations = violationCounts(examId);
        return records.stream().map(record -> {
            Student s = recordStudent(record);
            return new TeacherRecordDTO(
                    record.getStudentId(),
                    s == null ? "-" : s.getStudentNumber(),
                    s == null ? "-" : s.getName(),
                    s == null || s.getClassInfo() == null ? "-" : s.getClassInfo().getClassName(),
                    record.getTotalScore(),
                    record.getAttemptCount(),
                    violations.getOrDefault(record.getStudentId(), 0),
                    record.getPending() != null && record.getPending() == 1
            );
        }).toList();
    }

    /**
     * 一场考试的统计指标，可按班级筛选；无人参考返回零值
     */
    @Transactional(readOnly = true)
    public ExamStatsDTO stats(Long examId, Long classId) {
        // M7：待阅卷成绩为临时分，不进入均分/及格率/分数段统计
        List<BigDecimal> scores = filteredRecords(examId, classId).stream()
                .filter(r -> r.getPending() == null || r.getPending() != 1)
                .map(ExamRecord::getTotalScore)
                .toList();
        return summarize(scores);
    }

    /**
     * 统计纯逻辑：输入成绩集合 → 输出统计结果（无 I/O，便于单测）
     */
    public ExamStatsDTO summarize(List<BigDecimal> scores) {
        long attended = scores.size();
        if (attended == 0) {
            return new ExamStatsDTO(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, emptyBuckets());
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(new BigDecimal(attended), 1, RoundingMode.HALF_UP);
        BigDecimal max = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal min = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        long pass = scores.stream().filter(s -> s.compareTo(PASS_SCORE) >= 0).count();
        double passRate = new BigDecimal(pass * 100)
                .divide(new BigDecimal(attended), 1, RoundingMode.HALF_UP).doubleValue();
        return new ExamStatsDTO(attended, avg, max, min, passRate, bucketCounts(scores));
    }

    private List<ExamRecord> filteredRecords(Long examId, Long classId) {
        List<ExamRecord> records = examRecordRepository.findByExam_IdOrderByTotalScoreDesc(examId);
        if (classId == null) {
            return records;
        }
        return records.stream()
                .filter(r -> {
                    Student s = recordStudent(r);
                    return s != null && s.getClassInfo() != null && classId.equals(s.getClassInfo().getId());
                })
                .toList();
    }

    private Student recordStudent(ExamRecord record) {
        return studentRepository.findById(record.getStudentId()).orElse(null);
    }

    private Map<Long, Integer> violationCounts(Long examId) {
        Map<Long, Integer> map = new HashMap<>();
        for (ExamAttempt attempt : attemptRepository.findByExam_IdOrderByStartTimeDesc(examId)) {
            map.merge(attempt.getStudentId(),
                    attempt.getViolationCount() == null ? 0 : attempt.getViolationCount(), Integer::sum);
        }
        return map;
    }

    private List<ScoreBucketDTO> bucketCounts(List<BigDecimal> scores) {
        long[] counts = new long[BUCKET_LABELS.size()];
        for (BigDecimal s : scores) {
            counts[bucketIndex(s)]++;
        }
        List<ScoreBucketDTO> result = new ArrayList<>();
        for (int i = 0; i < BUCKET_LABELS.size(); i++) {
            result.add(new ScoreBucketDTO(BUCKET_LABELS.get(i).label(), counts[i]));
        }
        return result;
    }

    private int bucketIndex(BigDecimal score) {
        if (score.compareTo(new BigDecimal("60")) < 0) {
            return 0;
        }
        if (score.compareTo(new BigDecimal("70")) < 0) {
            return 1;
        }
        if (score.compareTo(new BigDecimal("80")) < 0) {
            return 2;
        }
        if (score.compareTo(new BigDecimal("90")) < 0) {
            return 3;
        }
        return 4; // >=90，含 100
    }

    private List<ScoreBucketDTO> emptyBuckets() {
        return BUCKET_LABELS;
    }
}
