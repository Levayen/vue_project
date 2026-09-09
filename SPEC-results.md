# Spec: results — 成绩查询、考后回顾与错题本

> 模块 id：`results` ｜ 依赖：grading（成绩与判分明细）｜ 地图：SPEC-exam-system-map.md

## Objective

交卷判分之后，为两类用户提供结果能力：
- **学生**：查看我的成绩列表、单次考试的题目回顾（我的答案 / 正确答案 / 解析）、错题本
- **教师**：查看某场考试的成绩列表、统计指标（均分、及格率、分数段分布），按班级筛选

**用户故事（学生）**
- 作为学生，我能看到我所有考试的成绩与状态。
- 作为学生，我能在考后回顾一次考试：每题题干、我的答案、正确答案、答案解析，错题一目了然。
- 作为学生，我有一个跨考试的错题本，集中查看我做错的题目及解析。

**用户故事（教师）**
- 作为教师，我能看到一场考试所有参考学生的成绩，按班级筛选。
- 作为教师，我能看到均分、最高分/最低分、及格率（60 分）与分数段人数分布。

> 回顾与错题本基于**已交卷**的判分数据：闭卷考试的答案在交卷后于本模块开放（答题阶段由 exam-session 脱敏）；开卷考试答题阶段即可见。未交卷时没有判分记录，自然无回顾内容。本模块保证学生只能访问**本人**数据。

## Project Structure（本模块新增）

```
backend/.../app/
├─ service/ResultService.java        # 学生成绩/回顾/错题本
├─ service/ExamStatsService.java     # 教师统计
├─ controller/StudentResultController.java
├─ controller/TeacherStatsController.java
└─ dto/ ExamReviewDTO.java, WrongQuestionDTO.java, ExamStatsDTO.java
frontend/src/
├─ api/result.ts
├─ views/student/MyScores.vue        # 成绩列表
├─ views/student/ExamReview.vue      # 单次回顾（对错标记 + 解析）
├─ views/student/WrongBook.vue       # 错题本
└─ views/teacher/ExamStats.vue       # 成绩与统计
```

本模块**不新增表**：读 grading 的 `exam_record`、`attempt_answer`，
关联 exam-paper 的 `paper_question` 快照（含 content/options/analysis 快照）与现有 student/classInfo。

> 注：错题本需要题干/选项/解析文本。`paper_question` 已快照 content/options/answer；
> **analysis（解析）需在 paper_question 快照中补充 `analysis_snapshot` 字段**
> （对 exam-paper spec 的快照字段增量，组卷时一并写入）。

## API 契约

学生（STUDENT，身份取自 token，只能查自己）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/student/results | 我的成绩列表：考试名、课程、有效成绩、次数、提交时间、是否及格 |
| GET | /api/student/results/{examId}/review | 单次考试回顾：题目列表（题干/选项/我的答案/正确答案/对错/解析），仅当本人该考试已有 SUBMITTED attempt |
| GET | /api/student/wrong-book?courseId= | 我的错题本：跨考试聚合 is_correct=0 的题目（答错/漏选/错选均为 0；含题干/我的答案/正确答案/解析/所属考试），可按课程筛选 |

教师（TEACHER/ADMIN）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/teacher/exams/{examId}/records?classId= | 成绩列表：学生、班级、成绩、次数、违规次数，可按班级筛选 |
| GET | /api/teacher/exams/{examId}/stats?classId= | 统计：参考人数、均分、最高/最低、及格率（≥60）、分数段 [0-60)/[60-70)/[70-80)/[80-90)/[90-100] |

权限与答案可见性：
- 学生 review / wrong-book 接口必须校验数据归属本人（attempt.studentId == 当前登录学生），否则 403
- 未交卷（无 SUBMITTED attempt）访问 review → 404（无判分记录可返回；闭卷考试答题阶段不提供答案）
- 已交卷后，回顾/错题本返回正确答案与解析（闭卷考试此时开放答案）
- 教师 stats 仅限自己课程的考试（校验 exam.course 归属，ADMIN 放行）

## 统计口径

```
参考人数  = count(exam_record where exam_id=?)          # 有有效成绩即参考
均分      = avg(total_score)，保留 1 位小数
及格率    = count(total_score >= 60) / 参考人数 * 100%
分数段    = 按 total_score 分桶计数
```
及格线常量化 `PASS_SCORE = 60`；分数段边界为常量数组，不散落魔法数字。

## Code Style

```java
// 错题本/回顾按 attempt_answer 关联快照组装 DTO，不向前端暴露实体
public record QuestionReviewDTO(
    String content, List<OptionDTO> options, String studentAnswer,
    String correctAnswer, int correctFlag, String analysis) {}
// 统计为纯计算方法，便于单测：输入成绩集合 → 输出统计结果
public ExamStatsDTO summarize(List<BigDecimal> scores) { ... }
```

## Testing Strategy

- `ExamStatsServiceTest`（纯逻辑重点）：
  - 给定一组成绩，均分/最高/最低/及格率/各分数段计数正确
  - 空成绩集合（无人参考）返回零值而非异常/除零
  - 边界：正好 60 分算及格；100 分落入 [90-100]
- `ResultServiceTest`：
  - 学生只能看自己：访问他人 review / wrong-book → 403
  - 未交卷访问 review → 404（无判分记录）
  - 错题本只聚合 is_correct=0 的题（漏选/错选均为 0，均计入错题）
- MockMvc 角色与越权校验
- 前端：vue-tsc 零错误；手测成绩列表、回顾页对错配色、错题本、教师统计图表（Element Plus 进度条/表格即可）

## Boundaries

- **Always**：学生数据严格隔离（只能查本人）；统计口径常量化；答案仅在已交卷回顾中返回（闭卷考试答题阶段脱敏由 exam-session 保证）
- **Ask first**：及格线/分数段调整；调整开卷/闭卷答案可见性策略
- **Never**：学生查看他人成绩；向未交卷的闭卷考试返回答案；统计接口返回与统计无关的学生隐私字段

## Success Criteria

- [ ] 学生可查成绩列表、交卷后可回顾每题对错与解析
- [ ] 错题本跨考试聚合错题，可按课程筛选
- [ ] 教师可按班级查看成绩与均分/及格率/分数段统计，无人参考时不报错
- [ ] 越权访问（他人成绩/回顾、错题本）被 403 拦截；未交卷回顾返回 404
- [ ] 统计纯逻辑单测覆盖正常值、边界值、空集合

## Open Questions

1. 错题本是否提供"已掌握"标记（学生手动移除）？（建议本期不做，保持只读）
2. 成绩是否需要导出 Excel？（建议本期不做，与题库导入一并列为后续增强）
