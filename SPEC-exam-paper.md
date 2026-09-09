# Spec: exam-paper — 试卷与组卷

> 模块 id：`exam-paper` ｜ 依赖：question-bank、identity ｜ 地图：SPEC-exam-system-map.md

## Objective

教师把题库题目组成一张试卷。支持两种组卷方式：
- **手动组卷**：从题库勾选题目、确定每题分值与顺序
- **随机组卷**：设定抽题规则（按题型 + 难度 + 数量 + 每题分值），系统从课程题库随机抽取

试卷一旦被考试引用，题目内容以**快照**形式固定，题库后续修改/删除不影响已发布试卷。

**用户故事**
- 作为教师，我能手动选题创建试卷并调整顺序和分值。
- 作为教师，我能设置规则让系统随机抽题组卷，并预览结果。
- 作为教师，我能看到试卷总分，组卷时系统校验分值与题目完整性。
- 作为教师，随机抽题数量超过题库存量时得到明确错误，而不是生成残缺试卷。

## Project Structure（本模块新增）

```
backend/.../app/
├─ entity/ExamPaper.java          # 试卷
├─ entity/PaperQuestion.java      # 试卷题目快照（关联表 + 冗余快照字段）
├─ repository/ExamPaperRepository.java
├─ repository/PaperQuestionRepository.java
├─ service/PaperService.java
├─ service/PaperGenerateService.java   # 随机抽题算法（可独立单测）
├─ controller/PaperController.java
└─ dto/ PaperDTO.java, RandomPaperRuleDTO.java
frontend/src/
├─ api/paper.ts
└─ views/teacher/PaperList.vue, PaperEdit.vue   # 手动选题 + 随机规则两种模式
```

## 数据模型

**exam_paper**（新表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| name | varchar(128) not null | 试卷名称 |
| course_id | bigint not null | 归属课程 |
| total_score | decimal(6,1) | 总分（组卷完成后计算冗余） |
| generate_type | varchar(16) | MANUAL / RANDOM |
| status | varchar(16) | DRAFT / PUBLISHED |
| create_by | bigint | 教师用户 id |
| create_time / update_time | datetime | |

**paper_question**（新表，试卷-题目关联 + 快照）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| paper_id | bigint not null | 外键 → exam_paper |
| question_id | bigint not null | 外键 → question |
| seq | int not null | 题目顺序 |
| score | decimal(5,1) not null | 本题在本卷分值（可覆盖题目默认分值） |
| content_snapshot | text not null | 题干快照 |
| options_snapshot | text null | 选项快照 JSON |
| answer_snapshot | varchar(255) not null | 正确答案快照（判分依据；发卷时按 exam.open_book 决定是否下发：闭卷脱敏、开卷下发） |
| type_snapshot | varchar(16) not null | 题型快照 |

快照在组卷时写入；考试与阅卷全部读快照表，与题库解耦。

**随机组卷规则**（请求 DTO，不落库或可落库供复用）

```json
{
  "name": "数据结构期中随机卷",
  "courseId": 3,
  "rules": [
    { "type": "SINGLE", "difficulty": 1, "count": 10, "scorePerQuestion": 2 },
    { "type": "MULTI", "difficulty": null, "count": 5,  "scorePerQuestion": 4 },
    { "type": "JUDGE",  "difficulty": null, "count": 5,  "scorePerQuestion": 2 },
    { "type": "FILL",   "difficulty": 2, "count": 5,  "scorePerQuestion": 4 }
  ]
}
```

## API 契约

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| GET | /api/papers?courseId=&page=&size= | TEACHER/ADMIN | 试卷列表 |
| GET | /api/papers/{id} | TEACHER/ADMIN | 试卷详情含题目（含答案，教师视图） |
| POST | /api/papers/manual | TEACHER | 手动组卷：`{name, courseId, items:[{questionId, seq, score}]}` |
| POST | /api/papers/random | TEACHER | 随机组卷，规则如上；题量不足返回 400 并说明缺口 |
| PUT | /api/papers/{id} | TEACHER | 编辑草稿（仅 DRAFT 可改） |
| DELETE | /api/papers/{id} | TEACHER | 删除草稿；已被考试引用返回 409 |
| POST | /api/papers/{id}/publish | TEACHER | DRAFT → PUBLISHED，发布后不可改 |

校验规则：
- 组卷后 `total_score = Σ paper_question.score`
- 每题 score > 0；同卷 seq 不重复
- 随机抽题：按规则在课程题库内 `WHERE type=? AND (difficulty=? OR ? IS NULL)` 随机排序取 count；
  可用题目数 < count 时整体失败（不生成半成品）

## Code Style

```java
// 抽题算法为纯函数式服务，不依赖 HTTP，便于单测
public List<Question> draw(Course course, DrawRule rule) {
    List<Question> pool = questionRepository.findByCourseAndType(course.getId(), rule.type());
    List<Question> filtered = rule.difficulty() == null ? pool
        : pool.stream().filter(q -> q.getDifficulty() == rule.difficulty()).toList();
    if (filtered.size() < rule.count()) {
        throw new BusinessException(400,
            "题型%s 需要%d题，题库仅%d题".formatted(rule.type(), rule.count(), filtered.size()));
    }
    Collections.shuffle(filtered);   // 随机抽取
    return filtered.subList(0, rule.count());
}
```

## Testing Strategy

- `PaperGenerateServiceTest`（重点，纯逻辑高覆盖）：
  - 规则全部满足 → 抽中数量/题型/难度正确，总分计算正确
  - 某题型题量不足 → 抛异常且不产生任何试卷数据（事务回滚）
  - difficulty=null 时跨难度抽取
  - 随机抽取不重复同一题
- `PaperServiceTest`：手动组卷快照写入正确；发布后编辑被拒；删除被引用试卷返回 409
- MockMvc：学生角色 403；教师正常
- 前端：手测手动选题拖拽/分值、随机组卷预览、草稿与发布状态

## Boundaries

- **Always**：考试/阅卷只读快照表；试卷归属课程；发布后只读；列表分页
- **Ask first**：修改快照结构；组卷规则新增维度（如知识点）
- **Never**：考试运行中读取题库实时数据；发布后再改试卷；题量不足时生成残缺试卷

## Success Criteria

- [ ] 手动与随机两种组卷方式均可生成试卷，总分自动计算
- [ ] 随机抽题题量不足时整体失败并提示缺口，无脏数据
- [ ] 题库题目被修改后，已发布试卷内容不变（快照生效）
- [ ] 已发布试卷不可编辑、被引用试卷不可删除
- [ ] PaperGenerateService 单测覆盖抽题成功/不足/去重/难度过滤分支

## Open Questions

1. 随机卷是否需要"每名学生题目顺序/选项顺序打乱"？（建议顺序打乱放在 exam-session 发卷时处理，本模块只生成固定卷）
2. 试卷是否支持复制（基于已有卷快速新建）？（建议列为后续增强）
