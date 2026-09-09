# Capability Map: 学生考试系统

> 状态：**待评审**（Phase 0 产物）。评审通过后按模块 id 逐一生成 `SPEC-<module-id>.md`，
> 再进入 Plan → Tasks → Implement。模块 id 一经确定不再重命名。

## 背景

在现有学生管理系统（学生 / 课程 / 班级 / 选课，Spring Boot 3 + Vue3 + Element Plus + MySQL）
基础上扩展在线考试能力。客观题自动阅卷，三角色（管理员 / 教师 / 学生），
支持考试时间窗口与倒计时、次数限制、防作弊记录、考后成绩与错题查看。

## 能力地图

| Module id | 职责 | 依赖 |
|---|---|---|
| `identity` | 账号、登录认证、三角色权限；学生账号关联现有 `student` 表；教师/管理员账号管理 | — |
| `question-bank` | 题库：单选 / 多选 / 判断 / 填空四类客观题，题干、选项、正确答案、分值、答案解析，按课程归类 | identity |
| `exam-paper` | 试卷与组卷：手动选题 + 按规则随机抽题（题型/数量/分值），总分校验，试卷归属课程 | question-bank, identity |
| `exam-session` | 在线考试：发布考试（时间窗口、时长、次数限制、**开卷/闭卷模式**）、学生入场校验（选课关系）、倒计时、到时自动交卷、答案定时暂存、防作弊事件记录；闭卷发卷脱敏、开卷发卷下发答案 | exam-paper, identity |
| `grading` | 自动阅卷：客观题判分（多选全对得分、漏选/错选一律 0 分）、成绩记录、次数成绩规则（最好成绩 / 末次成绩可配置） | exam-session |
| `results` | 成绩与回顾：学生查成绩 / 标准答案 / 解析 / 错题本；教师成绩列表与统计（均分、及格率、分数段） | grading |

**依赖方向无环**：identity → question-bank → exam-paper → exam-session → grading → results。

## 构建顺序

```
identity  →  question-bank  →  exam-paper  →  exam-session  →  grading  →  results
（前置）      （教师可录题）    （可组卷）     （学生可开考）    （交卷即出分）  （查分与错题）
```

- `grading` 逻辑上在交卷时同步触发，独立为模块是为了判分规则可单测、可演进
  （未来加入主观题人工阅卷时只扩展本模块）。
- `results` 依赖成绩数据，最后交付；其错题本数据在 grading 落库时即可生成。

## 与现有系统的关系

| 现有模块 | 考试系统复用方式 |
|---|---|
| student（学生） | identity 学生账号关联；成绩单显示学生信息 |
| course（课程） | 题库、试卷、考试均归属课程 |
| enrollment（选课） | 考试入场资格：选课学生才能参加该课程考试 |
| classInfo（班级） | 教师按班级查看成绩统计 |

## 本期不做（Out of Scope）

- 主观题（简答 / 论述）与人工阅卷工作台
- 题库批量导入导出（Excel/Word）
- 考试监控大屏、人脸识别等强防作弊手段
- 移动端 App / 小程序适配（响应式 Web 即可）

## 评审检查点

- [ ] 模块边界是否合理（grading 是否并入 exam-session？results 是否拆错题本？）
- [ ] 依赖方向与构建顺序是否认可
- [ ] Out of Scope 是否符合预期
