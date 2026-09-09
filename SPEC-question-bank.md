# Spec: question-bank — 题库管理

> 模块 id：`question-bank` ｜ 依赖：identity（教师权限）、现有 course ｜ 地图：SPEC-exam-system-map.md

## Objective

教师按课程维护客观题题库，支持四种题型：单选（SINGLE）、多选（MULTI）、判断（JUDGE）、
填空（FILL）。题目含题干、选项、正确答案、默认分值、答案解析、难度。题库是组卷的数据来源。

**用户故事**
- 作为教师，我能按课程增删改查题目，以便为课程积累题库。
- 作为教师，我能按题型/难度/关键词筛选题目，以便组卷时快速选题。
- 作为教师，我能为每道题填写答案解析，以便学生考后回顾。

## Tech Stack

沿用 Spring Boot 3 + JPA + MySQL / Vue3 + Element Plus。无新增依赖。

## Project Structure（本模块新增）

```
backend/.../app/
├─ entity/Question.java
├─ repository/QuestionRepository.java
├─ service/QuestionService.java
├─ controller/QuestionController.java
└─ dto/  QuestionDTO.java, QuestionQuery.java   # 入参/查询，区分教师/学生视图
frontend/src/
├─ api/question.ts
└─ views/teacher/QuestionBank.vue   # 题库列表 + 编辑弹窗（题型动态表单）
```

## 数据模型

**question**（新表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| course_id | bigint not null | 归属课程，外键 → course.id |
| type | varchar(16) not null | SINGLE / MULTI / JUDGE / FILL |
| content | text not null | 题干 |
| options | text null | 选项 JSON，如 `[{"key":"A","text":"..."}]`；判断题/填空题为空 |
| answer | varchar(255) not null | 正确答案，见下方"答案编码" |
| score | decimal(5,1) default 5 | 默认分值 |
| difficulty | tinyint default 1 | 1 易 / 2 中 / 3 难 |
| analysis | text null | 答案解析 |
| create_time / update_time | datetime | |

**答案编码约定**（判分模块 grading 共用，必须一致）
- SINGLE：单个选项 key，如 `"A"`
- MULTI：选项 key 排序后拼接，如 `"ABD"`
- JUDGE：`"T"` / `"F"`
- FILL：多个可接受答案用 `||` 分隔，如 `"北京||北京市"`；大小写/首尾空白判分时归一化

**字段级约束（Service 校验，@Valid + 业务校验）**
- SINGLE/MULTI：options 至少 2 项；answer 中的 key 必须存在于 options
- MULTI：answer 至少 2 个 key
- FILL：answer 非空
- score > 0 且 ≤ 100

## API 契约

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| GET | /api/questions?courseId=&type=&difficulty=&keyword=&page=&size= | TEACHER/ADMIN | 分页查询；返回不含 answer/analysis 的列表摘要可由教师视图带全字段 |
| GET | /api/questions/{id} | TEACHER/ADMIN | 题目详情（含答案/解析） |
| POST | /api/questions | TEACHER/ADMIN | 新建题目，校验题型规则 |
| PUT | /api/questions/{id} | TEACHER/ADMIN | 更新 |
| DELETE | /api/questions/{id} | TEACHER/ADMIN | 删除；**已被试卷引用时禁止删除**（返回 409，提示先从试卷移除） |

学生端不直接访问本题库管理接口（题库 CRUD 为教师/管理员权限）；考试中题目数据由 exam-session 下发，答案可见性按考试模式控制：闭卷（open_book=0）发卷脱敏、交卷后回顾可见；开卷（open_book=1）发卷即含答案/解析（策略详见 exam-session）。

## Code Style

```java
// 题型用枚举，禁止魔法字符串
public enum QuestionType { SINGLE, MULTI, JUDGE, FILL }

// 选项/答案的 JSON 序列化用集中工具，不散落 ObjectMapper
// Service 中按题型校验，违反则抛 BusinessException(400, "多选题答案至少2项")
```
```ts
// 前端题型表单按 type 动态渲染：判断题只渲染 正确/错误；填空题不渲染选项
const formRules = computed(() => typeRules[form.value.type])
```

## Testing Strategy

- `QuestionServiceTest`（重点）：
  - 四种题型的合法创建各 1 例
  - 非法：单选 answer 指向不存在选项、多选仅 1 个答案、填空答案为空、score=0 → 均抛业务异常
  - 删除已被试卷引用的题目 → 409（依赖 exam-paper 的引用表，本模块用接口/桩解耦）
- `QuestionControllerTest`（MockMvc）：学生角色 GET /api/questions → 403；教师 CRUD 正常
- 前端：vue-tsc 零错误；手测四种题型表单切换与校验提示

## Boundaries

- **Always**：答案编码严格遵守约定；题目归属课程；接口按角色授权；列表分页
- **Ask first**：修改答案编码约定（影响 grading）；增加新题型
- **Never**：硬删已被引用的题目；在 Controller 写业务校验；学生角色调用题库管理接口（应 403）

## Success Criteria

- [ ] 教师能按课程完成四种题型题目的增删改查与筛选
- [ ] 题型规则校验生效，非法题目无法保存
- [ ] 已被试卷引用的题目删除时被拒绝并给出明确提示
- [ ] QuestionService 单测覆盖全部合法/非法分支

## Open Questions

1. 题目是否需要"知识点/标签"分类字段？（建议本期不加，难度+课程已够用）
2. 题库批量导入（Excel）本期不做，确认？
