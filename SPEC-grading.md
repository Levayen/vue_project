# Spec: grading — 自动阅卷与成绩

> 模块 id：`grading` ｜ 依赖：exam-session（交卷触发）｜ 地图：SPEC-exam-system-map.md

## Objective

学生交卷后系统立即自动判分（客观题），落库每题判分结果与总分，按考试的成绩规则
（BEST 最好成绩 / LAST 末次成绩）确定有效成绩，并沉淀错题数据供 results 模块使用。
判分逻辑独立成模块，便于单测与未来扩展主观题人工阅卷。

**用户故事**
- 作为学生，我交卷后立刻看到本次得分。
- 作为学生，参加多次考试时，成绩按教师设定的规则（最好/末次）记录。
- 作为教师，我看到的每个学生成绩与系统判分一致、可追溯到每题对错。
- 作为系统，多选漏选/错选、填空多答案等判分规则有明确定义且被测试覆盖。

## Project Structure（本模块新增）

```
backend/.../app/
├─ entity/AttemptAnswer.java     # 每题判分明细
├─ entity/ExamRecord.java        # 一场考试一个学生的有效成绩（聚合 attempt）
├─ repository/AttemptAnswerRepository.java
├─ repository/ExamRecordRepository.java
├─ service/GradingService.java        # 判分入口（交卷时被 AttemptService 调用）
├─ service/AnswerMatcher.java         # 纯判分逻辑：按题型比对答案（核心可单测）
└─ controller/（本模块不直接暴露接口，结果经 results 模块查询）
frontend/src/  （成绩展示在 results 模块；交卷后跳转成绩页）
```

## 判分规则（与 question-bank 答案编码严格一致）

| 题型 | 正确判定 | 得分 |
|---|---|---|
| SINGLE 单选 | 学生答案 == 正确 key（归一化后） | 全对得满分，否则 0 |
| JUDGE 判断 | 学生答案 ∈ {"T","F"} 且 == 正确 | 全对或 0 |
| MULTI 多选 | 学生选项集合 vs 正确集合：**完全相等 → 满分；有错选或漏选均为 0 分**（不设半对/部分分） | 满分 / 0 |
| FILL 填空 | 归一化（去首尾空白、全角转半角、忽略大小写）后与任一可接受答案（`\|\|` 分隔）相等 → 满分，否则 0 | 满分或 0 |

- 未作答题目（answers_json 中无该 paperQuestionId）按 0 分、标记 `answered=false`。
- 多选不设部分分：必须与正确答案完全一致才得分，漏选、错选一律 0 分。

## 数据模型

**attempt_answer**（新表，每题判分明细）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| attempt_id | bigint not null | 外键 → exam_attempt |
| paper_question_id | bigint not null | 外键 → paper_question（快照题目） |
| question_type | varchar(16) | 题型快照 |
| student_answer | varchar(255) null | 学生答案编码（未答为 null） |
| correct_answer | varchar(255) not null | 正确答案快照 |
| is_correct | tinyint not null | 0 错 / 1 对（多选无半对，不使用 2；3 预留给未来主观题待评阅） |
| score | decimal(5,1) not null | 本题实得分 |
| full_score | decimal(5,1) not null | 本题满分 |

**exam_record**（新表，有效成绩聚合，一个学生一场考试一行）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| exam_id | bigint not null | |
| student_id | bigint not null | |
| total_score | decimal(6,1) not null | 有效成绩 |
| best_attempt_id | bigint null | BEST 规则下对应 attempt |
| last_attempt_id | bigint null | LAST 规则下对应 attempt |
| attempt_count | int default 0 | 已提交次数 |
| update_time | datetime | |

唯一约束：(exam_id, student_id)。交卷判分后 upsert：
- LAST：total_score = 本次分数，last_attempt_id = 本次
- BEST：total_score = max(历史, 本次)，best_attempt_id 指向最高分 attempt

## 处理流程

```
AttemptService.submit()
  └─ GradingService.grade(attempt)
       1. 读取 attempt.answers_json + 该卷 paper_question 快照（含 answer_snapshot/score）
       2. 逐题 AnswerMatcher.match(type, studentAnswer, correctAnswer) → 对/错 + 得分（多选全对或 0）
       3. 批量写 attempt_answer；总分 = Σ 得分
       4. attempt.status=SUBMITTED, submit_time=now
       5. 按 exam.score_rule upsert exam_record
       （整个过程在同一事务内，失败回滚，不产生半成品成绩）
```

## 关键代码风格

```java
// 判分是无副作用纯逻辑，输入输出明确，独立于 JPA/Spring，便于单测
public final class AnswerMatcher {
    // 多选：集合完全相等才得分，无部分分；判分为无状态纯逻辑
    public MatchResult match(QuestionType type, String student, String correct, BigDecimal full) { ... }
}
// 归一化工具集中一处：trim + 全角→半角 + 大小写
static String normalizeFill(String s) { ... }
```

## Testing Strategy（本模块为测试重点）

`AnswerMatcherTest`（纯单测，高覆盖，不依赖 Spring）：
- 单选：对/错/未答
- 判断：T/F 各种组合
- 多选：全对满分；含一个错选项 → 0；无错选但漏一项 → 0；漏多项 → 0；未答 0（无半对用例）
- 填空：精确匹配；首尾空格/大小写/全角差异归一化后匹配；多答案任一命中；不匹配 0

`GradingServiceTest`（@DataJpaTest 或 Mockito）：
- 交卷后 attempt_answer 题数与试卷一致；总分正确；attempt 状态 SUBMITTED
- LAST 规则多次提交取末次；BEST 规则取最高分且 best_attempt_id 正确
- 判分异常时事务回滚，无 attempt_answer/record 脏数据
- 未答题计 0 且 answered=false

## Boundaries

- **Always**：判分只读试卷快照；判分与交卷同事务；判分规则常量化、纯函数化
- **Ask first**：调整多选判分规则（如改为部分分）/填空归一化策略；成绩规则新增类型（如平均分）
- **Never**：在判分中实时读取题库；判分失败仍标记 SUBMITTED

## Success Criteria

- [ ] 交卷后立即得到总分，每题对错可在 attempt_answer 追溯
- [ ] 四种题型判分规则全部有单测且通过，含多选全对/漏选/错选与填空归一化边界
- [ ] BEST/LAST 成绩规则正确聚合到 exam_record
- [ ] 判分全程事务化，异常回滚无脏数据
- [ ] 未作答按 0 分处理且明确标记

## Open Questions

1. 及格线/等级（优/良/及格）是否在本模块计算？（建议放 results 统计，按教师配置 60 分及格）
2. 未来主观题人工阅卷：本模块预留 `is_correct=3 待评阅` 与人工评分入口，本期不实现，确认？
