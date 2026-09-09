# 实施计划：学生考试系统

> 依据：SPEC-exam-system-map.md（能力地图）+ SPEC-identity / question-bank / exam-paper /
> exam-session / grading / results 六份模块 spec ｜ 变更单：CC-20260907-003
> 状态：**待评审**（Phase 2 产物，评审通过后按 tasks/todo.md 执行）

## 1. 技术基线（沿用现有系统，不引入架构变更）

- 后端：Spring Boot 3.2.5 + JPA(Hibernate) + MySQL，包结构 `com.example.app.{entity,repository,service,controller,config,security,dto,exception}`
- 前端：Vue3 + TS + Vite + Element Plus，接口集中 `frontend/src/api/`，页面 `frontend/src/views/{teacher,student}/`
- 新增后端依赖：`io.jsonwebtoken:jjwt-api/impl/jackson:0.12.5`、`spring-security-crypto`（仅 BCrypt）
- 关键既有约定：实体手写 getter/setter、`/api/**` REST、统一 `GlobalExceptionHandler`、
  前端 axios 实例 + TS 接口类型

## 2. 模块依赖与构建顺序

```
identity ──▶ question-bank ──▶ exam-paper ──▶ exam-session ──▶ grading ──▶ results
 (认证)        (题库)           (试卷快照)      (考试/交卷)      (自动判分)    (成绩/错题)
```

每个模块内部按「后端实体 → repository → service → controller/DTO → 后端测试 → 前端 API → 前端页面」垂直切片交付，
**一个模块端到端跑通并过门禁后再进入下一个**，不一次性铺开所有层。

## 3. 里程碑与验证检查点

| 里程碑 | 包含任务 | 检查点（必须全绿才进入下一里程碑） |
|---|---|---|
| M0 identity | T1–T7 | 三角色可登录；未登录 401 / 越权 403；`mvn test` 通过；前端路由守卫生效 |
| M1 question-bank | T8–T11 | 教师可维护四题型题库；题型非法保存被拒；学生 403；`mvn test` + `vue-tsc` 通过 |
| M2 exam-paper | T12–T15 | 手动/随机组卷成功；题量不足整体失败无脏数据；快照隔离生效；测试通过 |
| M3 exam-session | T16–T21 | 发布考试→入场校验→倒计时→暂存→续考→强制交卷；闭卷/开卷载荷差异正确；测试通过 |
| M4 grading | T22–T25 | 交卷即出分；四题型判分（多选全对/漏选错选 0、填空归一化）单测全绿；BEST/LAST 聚合正确 |
| M5 results | T26–T28 | 成绩列表、回顾、错题本、教师统计；越权 403；统计纯逻辑单测通过 |
| M6 收尾 | T29–T30 | 角色菜单整合、种子数据；端到端走查；CI 三作业全绿 |

> M4 的 AnswerMatcher（T22）是无 Spring 依赖的纯逻辑，**可与 M3 并行**开发，
> 但 GradingService 事务集成（T24）必须等 exam-session 的交卷入口（T18）就绪。

## 4. 并行与串行

- **必须串行**：identity → 其余全部（没有认证无法做角色授权）；question-bank → exam-paper（题库是组卷数据源）；exam-paper → exam-session；exam-session 交卷 → grading 集成；grading → results。
- **可并行**：
  - T22 AnswerMatcher 纯判分逻辑 ‖ M3 考试会话
  - 各模块前端页面（接口契约以本计划/spec 的 API 表为准冻结后）可与后端实现并行
  - M5 教师统计纯逻辑 `summarize()` 可与 results 其他部分并行

## 5. 关键技术决策

1. **认证**：JWT（jjwt 0.12.x 新 API）+ 自研 HandlerInterceptor，不引入整套 Spring Security 过滤链；
   密钥走 `application.properties` 的 `app.jwt.secret`；BCrypt 哈希密码。
2. **时间**：注入 `java.time.Clock` Bean，考试窗口/截止时间判定全部经 Clock，单测可构造边界时刻。
3. **试卷快照**：组卷时把题干/选项/答案/解析/题型/分值复制进 `paper_question`；考试与判分只读快照。
4. **发卷脱敏**：`/start` 组装题目 DTO 时，`exam.open_book=0` 不输出 answer/analysis；=1 输出；判分始终读服务端快照。
5. **判分**：`AnswerMatcher` 静态纯函数 + 归一化工具，零 Spring 依赖，优先高覆盖单测；交卷判分与交卷同事务。
6. **答案暂存**：`exam_attempt.answers_json` 单字段 JSON；前端 30s/变更防抖保存；续考回显。
7. **DDL**：依赖 JPA `ddl-auto=update` 自动建表（与现有系统一致）；字段下划线命名由 `@Column(name=...)` 显式指定。

## 6. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| jjwt/security-crypto 新依赖与现有依赖冲突 | 后端起不来 | M0 第一步先加依赖并 `mvn compile`，早暴露；版本固定 |
| 答案脱敏被前端绕过（闭卷发卷仍含答案） | 考试失效 | 脱敏在服务端 DTO 组装完成；MockMvc 断言响应 JSON 无 answer 字段（T20） |
| 前端倒计时与服务端时间不一致 | 超时可作答 | deadline 服务端开考时算死；暂存/交卷接口服务端兜底强制判分，不信前端 |
| 随机组卷并发/题量不足产生半成品卷 | 脏数据 | 抽题不足整体抛异常 + `@Transactional` 回滚（T14 单测验证无残留） |
| 判分规则理解偏差（多选/填空） | 成绩错误 | 规则在 spec 冻结，AnswerMatcherTest 穷举边界用例先行（TDD） |
| 大改一次性铺开导致回归 | 现有学生管理功能受损 | 垂直切片 + 每里程碑过门禁；不动现有 CRUD 代码，只新增与注册拦截器 |

## 7. 数据模型落地顺序（建表先后）

1. `sys_user`（identity）
2. `question`（question-bank）
3. `exam_paper`、`paper_question`（exam-paper，含 `analysis_snapshot`）
4. `exam`（含 `open_book`）、`exam_attempt`（exam-session）
5. `attempt_answer`、`exam_record`（grading）
6. results 不新增表

## 8. 执行约定（Harness）

- 每个任务对应一次小步提交，提交信息遵守 Conventional Commits（如 `feat(identity): JWT登录与角色拦截器`）
- pre-commit 自动路由：后端改动跑 Maven 编译、前端改动跑 vue-tsc
- 任务状态在 `tasks/todo.md` 勾选；每里程碑完成更新变更单 CC-20260907-003 验证记录
- 任何与 spec 的偏差先改 spec 再写码（spec 是活文档）
