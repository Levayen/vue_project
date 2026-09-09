# 开发问题与经验记录（Lessons Learned）

> 本文档记录本项目（学生考试系统）开发过程中遇到的典型问题、根因、修复方案，
> 以及可转化为 Harness 规则 / 门禁 / 评审项的改进建议。
> 持续更新，新问题追加到对应分类末尾。

## 1. 后端（Spring Boot / JPA / Java）

### 1.1 JPA 关联实体保存顺序导致 TransientPropertyValueException

- **现象**：新建主实体（如 Paper）后立即保存引用它的子实体（如 PaperQuestion），子实体持有主实体的临时引用，JPA 报 `TransientPropertyValueException: object references an unsaved transient instance`。
- **根因**：主实体主键由数据库 `IDENTITY` 生成，未保存前 id 为 null，子实体外键无法解析。
- **修复**：先 `save(主实体)` 拿到持久化实例，再用该实例构建子实体的关联字段后 `saveAll(子实体)`。
- **Harness 建议**：在 `rules/backend-java-alibaba.md` 增加一条强制条目——"IDENTITY 主键的主从实体写入必须主实体先落库再构建子实体关联"；Reviewer 清单中检查"新建子实体时主实体是否已持久化"。

### 1.2 Spring Data JPA 派生查询命名陷阱

- **现象**：按 `question_id` 列查是否存在时写 `existsByQuestion_Id(questionId)` 不生效；按关联属性查才用 `Association_Id` 形式。
- **根因**：派生查询中普通列名直接用驼峰属性名（如 `existsByQuestionId`），下划线 `_` 仅用于解析嵌套关联属性路径。
- **修复**：普通列用 `existsByQuestionId`；关联对象的 id 用 `existsByQuestion_Id`。
- **Harness 建议**：Reviewer 清单中增加"JPA 派生查询命名检查"——下划线只用于关联路径拆解，普通列禁止加下划线。

### 1.3 Java 26 + Mockito 无法 mock 具体类

- **现象**：`Mockito.mock(PaperGenerateService.class)` 在 JDK 26 下失败（byte-buddy 不支持最新 JDK 的类结构）。
- **根因**：Mockito 通过 byte-buddy 字节码增强创建 mock，对高版本 JDK 的模块封装不兼容。
- **修复**：单测中改用真实实例 + mock 其依赖的仓库接口（Repository 是接口，mock 无问题）；或在 `pom.xml` 显式声明 `mockito-core` 版本与 byte-buddy 兼容范围。
- **Harness 建议**：在 `harness.yaml` 的 backend CI gate 中固定 Mockito 版本；或在规则中约定"mock 优先接口，避免 mock 具体类"。

### 1.4 @ConditionalOnMissingBean 导致 Bean 未注册

- **现象**：`DefaultGradingAdapter` 标注 `@ConditionalOnMissingBean(GradingPort.class)` 后自身不被 Spring 注册，导致 `GradingPort` bean 缺失、启动失败。
- **根因**：`@ConditionalOnMissingBean` 的判断时机——当 Bean 尚未注册时条件成立，但条件成立后该 Bean 注册又可能被后续同类型 Bean 覆盖；配置顺序不当导致桩实现和真实实现都没生效。
- **修复**：默认桩实现直接用 `@Component`（无条件注册），真实实现用 `@Primary` 或更高优先级覆盖；或明确依赖顺序。
- **Harness 建议**：Reviewer 清单中增加"条件 Bean 注册顺序检查"——`@ConditionalOnMissingBean` 的默认桩必须确保在真实实现之前被扫描，且真实实现有显式 `@Primary`。

### 1.5 全角空格归一化缺失

- **现象**：填空题答案 `normalizeFill` 将全角空格转半角后未再次 `trim()`，导致带全角空格的答案匹配失败。
- **根因**：归一化流水线（全角→半角→去标点→小写）末尾缺少最终 trim。
- **修复**：归一化函数末尾统一 `return sb.toString().trim().toLowerCase();`
- **Harness 建议**：规则中增加"字符串归一化函数必须以 trim 收尾"的编码约定；单元测试中覆盖"前后空白、全角空白"用例。

### 1.6 泛型捕获编译错误（ArgumentCaptor）

- **现象**：`ArgumentCaptor.forClass((Class) List.class)` 产生未检查转换警告，在严格编译下报错。
- **修复**：使用辅助方法 `@SuppressWarnings("unchecked") <T> ArgumentCaptor<List<T>> captor(Class<?> c) { return (ArgumentCaptor<List<T>>) ArgumentCaptor.forClass(c); }` 或用 `ArgumentCaptor.forClass(List.class)` 后显式转换。
- **Harness 建议**：在 `rules/backend-java-alibaba.md` 增加"泛型捕获需用 @SuppressWarnings 或辅助方法封装，禁止裸 (Class) 强转"。

### 1.7 校验先行原则（批量写入）

- **现象**：组卷等批量写入操作若逐条保存，中途失败会产生脏数据。
- **修复**：先完整校验全部条目（题目存在、分值合法、总分匹配），校验通过后再落库。
- **Harness 建议**：Reviewer 清单中增加"批量写入操作是否先全量校验再落库"。

### 1.8 考试时间字段时区格式

- **现象**：前端传 `2026-09-08T22:21:00Z`（带 Z），后端解析为 UTC 时间，与本地时间偏差 8 小时。
- **修复**：前端传本地时间格式 `yyyy-MM-ddTHH:mm:ss`（不带 Z），后端按 `Asia/Shanghai` 解析。
- **Harness 建议**：在 `rules/backend-java-alibaba.md` 增加"时间字段必须明确时区约定，禁止前后端默认时区不一致"；前端规则中增加"datetime-local 输出不带 Z"。

### 1.9 Service 构造函数新增依赖后单测编译失败

- **现象**：M6 为 `ExamService` 新增 `GradingPort` 和 `ObjectMapper` 两个构造参数后，`ExamServiceTest` 中 `new ExamService(examRepository, paperRepository, attemptRepository, enrollmentRepository)` 编译报错（参数数量不匹配）。
- **根因**：手写构造函数的 Service 新增依赖时，编译器会立即报测试文件构造调用不匹配，但容易在改完主代码后忘记同步更新测试，直到运行 `mvnw test` 才暴露。
- **修复**：同步更新测试的 `new ExamService(...)` 调用，为新增依赖补 `@Mock` 字段并传入。
- **Harness 建议**：Reviewer 清单中增加"Service 构造函数变更时，必须同步检查所有 `new XxxService(...)` 的测试调用点"；后端门禁中 `mvnw test-compile` 即可捕获，无需等到运行时。

### 1.10 JSON 字段解析容错（违规记录）

- **现象**：监考视图需解析 `ExamAttempt.violationsJson`（历史脏数据可能格式不合法），若 `ObjectMapper.readValue` 抛异常会导致整个监考接口 500。
- **根因**：text 字段存储 JSON 时，历史数据可能被截断或格式不规范，强解析会中断主流程。
- **修复**：解析逻辑用 try-catch 包裹，解析失败跳过该条记录（返回空违规列表），不影响监考主数据；日志中可选记录异常。
- **Harness 建议**：在 `rules/backend-java-alibaba.md` 增加"解析 text 字段中的 JSON 时必须 try-catch 容错，单条失败不影响主流程"。

## 2. 前端（Vue 3 / TypeScript / Element Plus）

### 2.1 el-dialog 在带 backdrop-filter 容器内被裁剪

- **现象**：`el-dialog` 在带 `backdrop-filter` / `overflow:hidden` 的父容器内被裁剪、遮罩层不居中。
- **根因**：Element Plus 的 dialog 默认渲染在当前组件树，受父级 stacking context 和 overflow 影响。
- **修复**：`el-dialog` 必须加 `append-to-body` 属性，渲染到 body 下脱离父容器约束。
- **Harness 建议**：在 `rules/frontend-vue-alibaba.md` 增加强制条目——"el-dialog / el-drawer / el-message-box 必须 append-to-body"；可加 ESLint 自定义规则检测。

### 2.2 前端类型检查缺 type 字段

- **现象**：`ExamReview.vue` 中题型渲染依赖 `q.type`，DTO 初始未包含该字段，`vue-tsc` 报错。
- **根因**：前后端 DTO 字段未对齐，前端接口类型定义遗漏。
- **修复**：在 `QuestionReviewDTO` 和 `WrongQuestionDTO` 中补 `type` 字段，后端 `ResultService` 中填充。
- **Harness 建议**：Reviewer 清单中增加"前后端 DTO 字段一致性检查"——前端 `api/*.ts` 中的 interface 字段必须与后端 DTO 完全对应。

### 2.3 多选题答案编码转展示文本陷阱

- **现象**：选项类答案编码可能是 `"AC"`（无分隔）或 `"C,A"`（逗号分隔），用 `split('')` 会把逗号当选项，用 `split(/[^A-Za-z]+/)` 对无分隔符的 `"AC"` 拆不开。
- **修复**：统一用 `code.match(/[A-Za-z]/g)` 逐字母提取。
- **Harness 建议**：在 `rules/frontend-vue-alibaba.md` 增加"选项答案编码解析必须用 match(/[A-Za-z]/g)，禁止 split('')"。

### 2.4 前端 vue-tsc 需在沙箱外运行

- **现象**：沙箱内 `npx vue-tsc` 因 npm 缓存路径问题失败。
- **修复**：在 `dangerouslyDisableSandbox: true` 下运行；npm 缓存位于 `E:\dsh\npm-cache`。
- **Harness 建议**：在 `AGENTS.md` 或 `harness-doctor.mjs` 中记录"前端类型检查需关闭沙箱或配置 npm-cache 路径"。

### 2.5 setInterval/setTimeout 定时器泄漏

- **现象**：M6 监考弹窗用 `setInterval(loadMonitoring, 10000)` 每 10 秒自动刷新，若不在弹窗关闭/组件卸载时 `clearInterval`，定时器会持续运行：弹窗关闭后仍发请求、组件销毁后回调访问已卸载的 ref。
- **根因**：Vue 组件内创建的定时器不会随组件销毁自动清除，需显式清理；弹窗场景还要在 dialog 关闭时清理（组件可能未销毁但弹窗已关）。
- **修复**：同时在 `onBeforeUnmount(() => stopTimer())` 和 `el-dialog @closed="stopTimer"` 两处清理；封装 `startTimer/stopTimer` 成对调用，start 时先 stop 旧定时器避免重复。
- **Harness 建议**：在 `rules/frontend-vue-alibaba.md` 增加强制条目——"组件内 `setInterval` 必须在 `onBeforeUnmount` 清理；弹窗内定时器还需在 dialog `@closed` 清理；startTimer 前先 clearInterval 防重复"。

## 3. 通用 / 工程

### 3.1 并行 Edit 同一文件导致冲突

- **现象**：在同一个 message 中对同一文件发起多个 `Edit` 调用，只有部分编辑生效，其余静默丢失。
- **根因**：并行 Edit 基于同一快照计算 diff，后写入的覆盖先写入的。
- **修复**：对同一文件的多个 Edit 必须串行执行；或一次性用 `Write` 重写整个文件。
- **Harness 建议**：在 `agents/developer.md` 中增加"禁止对同一文件并行调用 Edit"的操作约定。

### 3.2 Write 工具未真正落盘

- **现象**：`Write` 返回成功但磁盘文件内容仍是旧版本（在某些环境下偶发）。
- **修复**：写入后用 `Read` 或 `Select-String` 验证磁盘内容；或用 `Shell` 的 `Set-Content` 直接写盘。
- **Harness 建议**：在 `agents/developer.md` 中增加"关键文件写入后必须验证磁盘内容"的操作约定。

### 3.3 接口字段易错点清单

以下是本项目中容易传错的接口字段，Reviewer 评审接口对接时需重点核对：

| 接口 | 易错点 | 正确写法 |
|------|--------|----------|
| `GET /api/questions` | 返回值是 Page 对象 | 用 `.content` 取数组，不是直接当数组 |
| `GET /api/papers/{id}` | 题目列表字段名 | `items`，不是 `questions` |
| `POST /api/papers/random` | 规则分数字段 | `scorePerQuestion`，不是 `score` |
| `POST /api/admin/users/{id}/reset-password` | 请求体字段 | `{ password }`，不是 `{ newPassword }` |
| `POST /api/exams` | 时间字段格式 | `yyyy-MM-ddTHH:mm:ss`（不带 Z） |

- **Harness 建议**：将此表纳入 `agents/reviewer.md` 的评审清单，作为接口对接的必查项。

### 3.4 端到端冒烟脚本模式

- **现象**：每次模块完成后需要手动验证全链路，重复劳动多。
- **模式**：Node 全局 `fetch` + `/api` 前缀直连后端 8080；教师 `teacher/teacher123`、学生 `学号/student123`；流程覆盖"建题→组卷→发考→答题→交卷→判分→成绩→统计"。
- **Harness 建议**：在 `scripts/` 下固化一个 `e2e-smoke.mjs` 模板，作为 CI 的可选冒烟门禁（非阻断）。

### 3.5 成绩列表 submitTime 为 null

- **现象**：`ResultService.toResultDTO` 中未正确解析有效尝试的 `submitTime`。
- **修复**：在 `resolveValidAttempt` 中获取 attempt 并返回 `submitTime`。
- **Harness 建议**：Reviewer 清单中增加"DTO 时间字段是否在 service 层正确填充"。

### 3.6 冒烟测试副作用污染共享账号

- **现象**：M5 冒烟脚本调用"重置密码"接口把 `teacher` 密码改成了 `reset123`，脚本结束后未恢复；M6 冒烟时用 `teacher/teacher123` 登录直接返回 401，排查半天才发现是上一个脚本的副作用。
- **根因**：冒烟脚本对共享演示账号（admin/teacher）做了修改类操作（重置密码、改数据），但没有在 finally 中恢复状态；多次运行或跨模块复用时，残留状态导致后续脚本失败。
- **修复**：① 冒烟脚本中重置密码应选非 admin/teacher 的测试用户（或用运行时新建的临时用户）；② 若必须改共享账号，在脚本末尾恢复原密码；③ 脚本失败时也打印当前登录态，便于定位。
- **Harness 建议**：在 `agents/developer.md` 的测试约定中增加"冒烟/集成脚本禁止修改共享演示账号（admin/teacher）密码；确需修改必须在 finally 恢复；优先用运行时新建的临时数据"。

### 3.7 统计字段语义需在 DTO 注释中明确

- **现象**：M6 冒烟测试断言 `enrolledCount === 1` 失败，实际返回 3。`enrolledCount` 的语义是"课程选课总人数"（应考人数），而测试误以为是"本场考试已开考人数"。
- **根因**：统计类 DTO 字段名相近（enrolledCount / inProgressCount / submittedCount），语义边界仅凭字段名容易误判；测试断言基于错误的语义假设。
- **修复**：在 DTO record 字段上写清注释（`enrolledCount` = 选课参考人数，`inProgressCount` = 当前进行中）；测试断言前先确认字段语义。
- **Harness 建议**：Reviewer 清单中增加"统计/聚合类 DTO 字段必须有注释说明统计口径（总数/当前数/累计数）"。

## 4. 架构 / 设计模式

### 4.1 模块间引用解耦模式

- **模式**：模块间引用检查用 Port 接口 + `@ConditionalOnMissingBean` 默认桩（`QuestionReferencePort` / `PaperReferencePort` 均如此），下一模块提供真实 Bean 自动替换。
- **注意**：见 1.4，默认桩不要加 `@ConditionalOnMissingBean`，直接 `@Component`，真实实现用 `@Primary`。

### 4.2 服务端时间控制

- **模式**：考试倒计时完全依赖服务端 `deadline`，前端仅展示；交卷/暂存接口强制超时校验。
- **Harness 建议**：规则中增加"涉及时间窗口的业务逻辑必须以服务端时间为准，前端计时仅展示"。

### 4.3 防作弊事件监听

- **模式**：监听 `visibilitychange` / `blur` / `copy` / `paste` / `fullscreen` 事件并上报违规行为。

### 4.4 答案防抖暂存

- **模式**：答题页实现防抖 5s 自动暂存，支持断点续考。

## 5. 判分规则（业务约束）

- 多选题：完全匹配答案才得满分，错选/漏选一律 0 分。
- 判断题：答案编码 `T` / `F`（不是 `TRUE` / `FALSE`，也不是 `对` / `错`）。
- 填空题：归一化（全角→半角、去标点、小写、trim）后比对。
- 开卷/闭卷：`exam.open_book` 字段（0=闭卷默认，1=开卷），闭卷发卷时服务端脱敏 `answer_snapshot` 和解析字段。

- **Harness 建议**：将判分规则作为 `rules/` 下的业务规则文档固化，Reviewer 评审判分逻辑时逐条核对。

---

## 更新日志

| 日期 | 追加内容 |
|------|----------|
| 2026-09-08 | 初始版本，整理 M1-M5 全量开发问题与经验 |
| 2026-09-08 | 落地规则到 harness：后端规则（IDENTITY 主从保存、JPA 派生查询命名、批量校验先行、归一化 trim、Port 解耦、服务端时间）、前端规则（答案编码 match、DTO 字段一致性）、Reviewer 清单（新增后端 5 项、前端 2 项、接口易错字段 5 项）、Developer 工具操作约定（禁止并行 Edit、写入后验证磁盘） |
| 2026-09-08 | M6 考试监控与监考模块问题：1.9 Service 构造函数变更需同步单测、1.10 JSON 字段解析容错、2.5 setInterval 定时器泄漏清理、3.6 冒烟测试副作用污染共享账号、3.7 统计字段语义需在 DTO 注释明确 |
