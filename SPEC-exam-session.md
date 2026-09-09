# Spec: exam-session — 在线考试与会话控制

> 模块 id：`exam-session` ｜ 依赖：exam-paper、identity、现有 enrollment/course ｜ 地图：SPEC-exam-system-map.md

## Objective

教师基于已发布试卷**发布考试**并设定规则；学生在规定时间窗口内入场、答题、交卷。
系统负责入场资格校验、倒计时与到时自动交卷、答案定时暂存、考试次数控制、防作弊事件记录。

**用户故事（教师）**
- 作为教师，我能选择试卷发布考试，设定开始/结束时间、考试时长、最多考试次数、成绩规则。
- 作为教师，我能设定考试为**闭卷（正式，默认）**或**开卷（练习）**：开卷考试学生答题时即可看到答案与解析，闭卷考试交卷后才在回顾中显示。
- 作为教师，我能看到某场考试的参考学生、交卷状态与防作弊记录。

**用户故事（学生）**
- 作为学生，我能看到我可选课程的进行中/未开始考试列表。
- 作为学生，开考后我看到倒计时，剩余时间为 0 时系统自动交卷；答题过程中答案自动暂存，刷新不丢失。
- 作为学生，未选课、不在时间窗口、次数用尽时，我得到明确提示且无法进入考试。
- 作为学生，切屏/离开全屏/粘贴被记录并被提醒，但不会被强制交卷。
- 作为学生，参加开卷（练习）考试时我在答题页就能看到答案解析；正式考试答题时看不到，交卷后可在成绩回顾中查看。

## Project Structure（本模块新增）

```
backend/.../app/
├─ entity/Exam.java               # 一场考试
├─ entity/ExamAttempt.java        # 学生一次考试尝试
├─ repository/ExamRepository.java
├─ repository/ExamAttemptRepository.java
├─ service/ExamService.java       # 发布/列表/入场校验
├─ service/AttemptService.java    # 开考、暂存、交卷（交卷时调用 grading）
├─ controller/ExamController.java      # 教师/管理员
├─ controller/StudentExamController.java # 学生考试中接口
└─ dto/ ExamDTO.java, SubmitAnswerDTO.java, SaveAnswersDTO.java
frontend/src/
├─ api/exam.ts
├─ views/teacher/ExamManage.vue        # 发布考试、监考列表
└─ views/student/ExamList.vue, ExamRoom.vue  # 考试列表 + 答题页（倒计时/暂存/防作弊）
```

## 数据模型

**exam**（新表，一场考试）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| paper_id | bigint not null | 外键 → exam_paper（须 PUBLISHED） |
| course_id | bigint not null | 冗余自试卷课程，便于查询 |
| title | varchar(128) not null | 考试名称 |
| start_time | datetime not null | 时间窗口起 |
| end_time | datetime not null | 时间窗口止（晚于此不可入场/自动截止） |
| duration_minutes | int not null | 考试时长（分钟），用于倒计时 |
| max_attempts | int default 1 | 最大考试次数 |
| score_rule | varchar(8) default 'LAST' | BEST 取最好成绩 / LAST 取末次 |
| shuffle | tinyint default 1 | 是否打乱题目顺序 |
| open_book | tinyint default 0 | **开卷模式**：1=开卷（练习）考试，发卷时下发答案/解析；0=闭卷（正式）考试，发卷脱敏，交卷后才在回顾中显示答案 |
| create_time / update_time | datetime | |

约束：`end_time > start_time`；`duration_minutes > 0`；有效考试时长 =
`min(个人开考时刻 + duration, end_time)`，即到 end_time 强制结束。

**exam_attempt**（新表，一次考试尝试）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| exam_id | bigint not null | |
| student_id | bigint not null | 关联 student.id |
| status | varchar(16) not null | IN_PROGRESS / SUBMITTED |
| start_time | datetime not null | 实际开考时刻 |
| deadline | datetime not null | 本次截止 = min(start+duration, exam.end_time)，开考时计算 |
| submit_time | datetime null | 交卷时刻 |
| answers_json | mediumtext | 暂存答案：`{"<paperQuestionId>": "答案编码"}` |
| violation_count | int default 0 | 切屏/失焦等违规次数 |
| violations_json | text | 违规事件明细（类型+时间戳） |
| create_time / update_time | datetime | |

唯一约束：同一 (exam, student, 尝试序号)；次数按 count(status=SUBMITTED) 或全部 attempt 统计
（**取已提交次数**，进行中崩溃的不算次数，允许重新进入续考）。

## 关键业务规则

**入场校验**（全部满足才允许开考）
1. 学生已选该课程（enrollment 存在）
2. 当前时间在 [start_time, end_time) 内
3. 已提交尝试次数 < max_attempts
4. 不存在他人替考（token 中学生身份与路径一致）

**续考**：若该生存在 status=IN_PROGRESS 的 attempt 且未过 deadline，重新进入时**恢复**该 attempt
与已暂存答案，倒计时以 deadline 为准（不重置）。

**自动交卷**：
- 前端倒计时到 0 自动调用交卷
- **服务端兜底**：交卷/暂存接口若发现 `now > deadline`，按已暂存答案强制判分提交（不信任前端时间）

**防作弊（记录性质）**：前端监听 `visibilitychange`/`blur`（切屏）、`copy`/`paste`、
退出全屏；每次上报 `POST /api/student/exams/{examId}/violations`，累加计数并记录明细。
不强制交卷，次数在教师监考视图展示。

**答案暂存**：答题页每 30 秒或每次作答变更（防抖 5 秒）调用保存接口；恢复考试时回显。

## API 契约

教师/管理员（TEACHER/ADMIN）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/exams | 发布考试（校验试卷已发布、时间合法）；body 含 `openBook`（默认 false=闭卷）、shuffle、scoreRule、maxAttempts 等规则 |
| GET | /api/exams?courseId= | 考试列表 |
| GET | /api/exams/{id}/monitoring | 参考学生、attempt 状态、违规次数 |

学生（STUDENT，身份取自 token）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/student/exams | 我的考试列表（含状态：未开始/可考/已截止/次数用尽 + 已考成绩） |
| POST | /api/student/exams/{examId}/start | 入场校验 → 新建或恢复 attempt；返回题目（快照、按 shuffle 顺序）、deadline、剩余秒数；**题目载荷按 open_book 决定是否含答案**（见下方答案可见性策略） |
| PUT | /api/student/exams/{examId}/answers | 暂存答案（仅 IN_PROGRESS 且未过 deadline） |
| POST | /api/student/exams/{examId}/violations | 上报违规事件 |
| POST | /api/student/exams/{examId}/submit | 交卷 → 触发 grading 判分 → 返回成绩；过 deadline 按暂存答案强制交 |

**答案可见性策略（按考试模式区分）**
- **闭卷考试（open_book=0，默认/正式）**：`/start` 返回的题目载荷**脱敏**——裁掉 `answer_snapshot` 与解析字段，学生在答题时及网络请求中都看不到答案；交卷后由 results 模块的回顾接口返回答案与解析。
- **开卷考试（open_book=1，练习）**：`/start` 返回的题目载荷**包含** `answer_snapshot` 与解析，学生答题时即可查看（课堂练习/自测场景）。
- 脱敏在发卷组装 DTO 时完成（服务端控制，不信任前端）；判分始终使用服务端快照答案，与是否下发无关。

> 决策记录（2026-09-07）：曾按需求移除全部脱敏，后确认为「开卷/闭卷双模式」——正式考试保留脱敏屏障，练习考试开放答案，通过 `exam.open_book` 开关切换。

## Code Style

```java
// 时间一律以服务端为准；截止时刻开考时算死，防前端改时间
LocalDateTime deadline = min(startTime.plusMinutes(exam.getDurationMinutes()),
                             exam.getEndTime());
// 交卷与暂存都做 deadline 兜底
if (LocalDateTime.now().isAfter(attempt.getDeadline())) {
    return gradingService.forceSubmit(attempt); // 按已暂存答案判分
}
```
```ts
// 倒计时基于服务端返回的 deadline 与本地时钟差计算，不依赖纯本地计时
const remaining = computed(() =>
  Math.max(0, Math.floor((deadline.value - Date.now() + serverClockOffset) / 1000)))
onBeforeUnmount(() => stopAutoSave())  // 离开答题页清理定时器与监听
```

## Testing Strategy

- `ExamServiceTest`：
  - 发布考试：试卷未发布 → 拒绝；end ≤ start → 拒绝
  - 入场：未选课/未到时间/过截止/次数用尽 → 各自抛出明确业务异常
  - 续考：IN_PROGRESS attempt 恢复且 deadline 不变
- `AttemptServiceTest`：
  - 正常暂存/交卷；过 deadline 交卷按暂存内容强制判分（与 grading 集成）
  - 次数统计：IN_PROGRESS 崩溃记录不占用次数
  - **答案可见性**：open_book=0 时 /start 返回题目不含 answer/analysis 字段；open_book=1 时包含；两种模式判分结果一致
- 时间相关用可注入的 `Clock`，单测构造时间窗口边界
- MockMvc：学生访问 /api/exams（教师接口）→ 403；学生 A 不能提交学生 B 的考试；闭卷 /start 响应 JSON 断言无 answer 字段
- 前端：手测倒计时归零自动交卷、刷新答案回显、切屏计数、越权拦截、开卷考试答题页显示解析而闭卷不显示

## Boundaries

- **Always**：时间/身份以服务端为准；题目读快照表；状态机只允许 IN_PROGRESS→SUBMITTED；闭卷考试发卷必须脱敏（open_book=0 时不返回 answer/analysis）
- **Ask first**：考试规则新增维度（如 IP 限制、乱序规则细化）；改动截止时间语义；修改开卷/闭卷默认值
- **Never**：信任前端倒计时/本地时间决定交卷；允许过截止后继续作答；闭卷考试在交卷前向前端下发答案

## Success Criteria

- [ ] 教师可发布考试并在监考视图看到参考状态与违规次数
- [ ] 学生仅能在选课 + 时间窗口 + 次数内开考，越界均有明确提示
- [ ] 倒计时基于服务端 deadline，归零自动交卷；服务端对超时交卷强制判分
- [ ] 答题中刷新/重进可恢复答案与剩余时间；崩溃未交不占用考试次数
- [ ] 切屏/粘贴等违规被记录且不强制交卷
- [ ] 闭卷考试 /start 载荷不含 answer/analysis；开卷考试含 answer/analysis；教师发布时可切换模式
- [ ] 入场校验、截止时间、次数规则、开卷/闭卷脱敏的服务端单测全覆盖

## Open Questions

1. 到 end_time 时仍有学生 IN_PROGRESS，是否需要定时任务批量收卷？（建议：暂以"下次请求兜底 + 交卷接口处理"覆盖；定时收卷列为增强，因为交卷/暂存都会触发兜底）
2. 多次考试时学生是否能看到历史每次成绩？（建议 results 模块展示，本模块只存全部 attempt）
