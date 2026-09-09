# 任务清单：学生考试系统

> 执行顺序自上而下（按依赖排序）。每任务 ≤5 文件，完成后勾选并按 Conventional Commits 小步提交。
> Verify 列是该任务的验证动作；里程碑末统一跑 `mvn test` + `vue-tsc --noEmit`。

## M0 identity — 认证与权限

- [x] **T1 引入认证依赖与配置**
  - Acceptance：pom 含 jjwt 0.12.5 与 spring-security-crypto；`app.jwt.secret` 在 application.properties；应用正常启动
  - Verify：`mvnw.cmd -q -DskipTests compile` 通过；启动日志无依赖冲突
  - Files：backend/pom.xml、backend/src/main/resources/application.properties

- [x] **T2 SysUser 实体与仓库**
  - Acceptance：sys_user 表字段齐（username 唯一、password_hash、role、student_id 唯一可空、enabled、审计时间）；role=STUDENT 时 student_id 必填的约束在 Service 层校验
  - Verify：启动后表自动创建；Repository 方法 findByUsername/findByStudentId 可用
  - Files：entity/SysUser.java、repository/SysUserRepository.java

- [x] **T3 JWT 工具与拦截器骨架**
  - Acceptance：JwtUtil 可签发/解析/校验（claims: uid,username,role，8h 过期）；AuthInterceptor 校验 token 并写入 UserContext(ThreadLocal)；WebConfig 注册拦截器并放行 /api/auth/login
  - Verify：`JwtUtilTest` round-trip、过期/篡改 token 失败用例通过
  - Files：security/JwtUtil.java、security/AuthInterceptor.java、security/UserContext.java、config/WebConfig.java、src/test/.../JwtUtilTest.java

- [x] **T4 登录服务与接口**
  - Acceptance：POST /api/auth/login 返回 {token,user}；错误密码/停用账号 401；GET /api/auth/me 返回当前用户；BusinessException 经 GlobalExceptionHandler 输出 {code,message}
  - Verify：MockMvc 登录成功/失败用例；密码 BCrypt 比对，日志无密码/token
  - Files：service/AuthService.java、controller/AuthController.java、dto/LoginRequest.java、dto/LoginResponse.java、exception/GlobalExceptionHandler.java

- [x] **T5 账号管理接口（管理员）**
  - Acceptance：ADMIN 可列表/创建/重置密码/启停/删除账号；创建学生账号可传 studentId 并以学号为用户名；不可删自己；非 ADMIN 访问 403
  - Verify：MockMvc 角色用例（TEACHER/STUDENT 403，ADMIN 200）
  - Files：controller/SysUserController.java、service/SysUserService.java(可并入AuthService)、dto/SysUserDTO.java、repository（T2 复用）

- [x] **T6 identity 后端测试补全**
  - Acceptance：AuthServiceTest（登录/停用/重复用户名/学生关联）、AuthInterceptorTest（无 token 401、角色不足 403）全绿
  - Verify：`mvnw.cmd -q test` 全通过
  - Files：src/test/.../AuthServiceTest.java、src/test/.../AuthInterceptorTest.java

- [x] **T7 前端登录与路由守卫**
  - Acceptance：Login.vue 可登录并存储 token；axios 请求自动带 Authorization 头、401 跳登录；router meta.roles + 全局守卫按角色放行/跳 403；auth store 暴露当前用户/角色
  - Verify：`vue-tsc --noEmit` 零错误；手测三角色登录看到不同菜单、越权 URL 被拦
  - Files：frontend/src/api/auth.ts、frontend/src/stores/auth.ts、frontend/src/views/Login.vue、frontend/src/router/index.ts、frontend/src/api/axios.ts

## M1 question-bank — 题库

- [x] **T8 Question 实体与仓库**
  - Acceptance：question 表字段齐（course_id、type、content、options、answer、score、difficulty、analysis、审计时间）；支持按课程/题型/难度/关键词分页查询
  - Verify：ddl-auto 建表；Repository 查询方法可用
  - Files：entity/Question.java（+QuestionType 枚举）、repository/QuestionRepository.java

- [x] **T9 题库服务与接口（教师）**
  - Acceptance：GET 分页筛选 / GET 详情 / POST / PUT / DELETE；四题型规则校验（选项≥2、answer key 存在、多选≥2 key、填空非空、0<score≤100）；删除已被试卷引用题目返回 409；TEACHER/ADMIN 可访问，STUDENT 403
  - Verify：QuestionServiceTest 合法/非法分支通过；MockMvc 学生 403
  - Files：service/QuestionService.java、controller/QuestionController.java、dto/QuestionDTO.java、dto/QuestionQuery.java、src/test/.../QuestionServiceTest.java

- [x] **T10 前端题库管理页**
  - Acceptance：QuestionBank.vue 按课程筛选、列表分页；新增/编辑弹窗按题型动态渲染表单（判断=对错、填空=无选项、多选=多答案）；非法提交有校验提示；删除引用冲突显示 409 消息
  - Verify：vue-tsc 零错误；手测四题型增删改查与报错提示
  - Files：frontend/src/api/question.ts、frontend/src/views/teacher/QuestionBank.vue

## M2 exam-paper — 试卷与组卷

- [ ] **T11 试卷实体与快照表**
  - Acceptance：exam_paper（name,course,total_score,generate_type,status,create_by）与 paper_question（paper_id,question_id,seq,score,content/options/answer/type/analysis 快照）建表；快照字段完整
  - Verify：ddl-auto 建表；Repository 可用
  - Files：entity/ExamPaper.java、entity/PaperQuestion.java、repository/ExamPaperRepository.java、repository/PaperQuestionRepository.java

- [ ] **T12 手动组卷与发布**
  - Acceptance：POST /api/papers/manual 写入题目快照并计算总分；DRAFT 可编辑、PUBLISHED 只读；发布接口 DRAFT→PUBLISHED；删除被考试引用试卷 409；seq 不重复、score>0 校验
  - Verify：PaperServiceTest 快照写入/发布后拒改/引用删除 409 通过
  - Files：service/PaperService.java、controller/PaperController.java、dto/PaperDTO.java、src/test/.../PaperServiceTest.java

- [ ] **T13 随机组卷算法**
  - Acceptance：POST /api/papers/random 按规则（题型/难度/数量/每题分）抽题；题量不足整体 400 并提示缺口、事务回滚无残留；抽题不重复；difficulty=null 跨难度
  - Verify：PaperGenerateServiceTest 覆盖 成功/不足回滚/去重/难度过滤/总分计算
  - Files：service/PaperGenerateService.java、dto/RandomPaperRuleDTO.java、src/test/.../PaperGenerateServiceTest.java、controller/PaperController.java（T12 复用扩展）

- [ ] **T14 前端试卷管理页**
  - Acceptance：PaperList 展示试卷与状态；PaperEdit 支持手动选题（题库多选+分值+顺序）与随机规则两种模式；随机失败显示缺口提示；发布按钮二次确认
  - Verify：vue-tsc 零错误；手测两种组卷与草稿/发布状态
  - Files：frontend/src/api/paper.ts、frontend/src/views/teacher/PaperList.vue、frontend/src/views/teacher/PaperEdit.vue

## M3 exam-session — 在线考试

- [ ] **T15 考试与会话实体**
  - Acceptance：exam 表（paper_id,course_id,title,start/end_time,duration_minutes,max_attempts,score_rule,shuffle,open_book 默认0）与 exam_attempt 表（exam_id,student_id,status,start_time,deadline,submit_time,answers_json,violation_count,violations_json）建表
  - Verify：ddl-auto 建表；Clock Bean 配置
  - Files：entity/Exam.java、entity/ExamAttempt.java、repository/ExamRepository.java、repository/ExamAttemptRepository.java、config/ClockConfig.java

- [ ] **T16 考试发布与入场校验服务**
  - Acceptance：POST /api/exams（校验试卷 PUBLISHED、end>start，body 含 openBook 等）；入场校验四条件（选课/时间窗口/次数/本人）；考试列表与监考接口；越界均抛明确业务异常
  - Verify：ExamServiceTest 发布非法/入场四种拒绝/续考恢复 deadline 不变 通过
  - Files：service/ExamService.java、controller/ExamController.java、dto/ExamDTO.java、src/test/.../ExamServiceTest.java

- [ ] **T17 开考/暂存/交卷会话服务（含脱敏）**
  - Acceptance：/start 新建或恢复 attempt，deadline=min(开始+duration,end)；题目读快照并按 open_book 决定是否含 answer/analysis（闭卷脱敏）；/answers 暂存（仅 IN_PROGRESS 且未过 deadline）；/violations 累加；/submit 交卷触发 grading；过 deadline 按暂存答案强制判分；已提交次数统计不含崩溃中的 IN_PROGRESS
  - Verify：AttemptServiceTest 覆盖暂存/续考/超时强制交卷/次数统计/闭卷无 answer 字段而开卷有/两模式判分一致
  - Files：service/AttemptService.java、controller/StudentExamController.java、dto/StartExamDTO 等、src/test/.../AttemptServiceTest.java

- [ ] **T18 前端教师考试管理**
  - Acceptance：ExamManage.vue 可选试卷发布考试（设置时间/时长/次数/成绩规则/开卷闭卷开关/乱序）；监考视图显示参考状态与违规次数
  - Verify：vue-tsc 零错误；手测发布一场闭卷与一场开卷考试
  - Files：frontend/src/api/exam.ts、frontend/src/views/teacher/ExamManage.vue

- [ ] **T19 前端学生考试大厅与答题页**
  - Acceptance：ExamList 显示考试状态（未开始/可考/已截止/次数用尽）；ExamRoom 基于服务端 deadline 倒计时、归零自动交卷；作答防抖+30s 自动暂存；刷新恢复答案与剩余时间；切屏/失焦/粘贴计数并提醒上报；开卷考试显示解析、闭卷不显示
  - Verify：手测倒计时自动交卷、刷新回显、切屏计数、闭卷无解析开卷有
  - Files：frontend/src/views/student/ExamList.vue、frontend/src/views/student/ExamRoom.vue、frontend/src/api/exam.ts（T18 复用扩展）

## M4 grading — 自动阅卷（T20 可与 M3 并行）

- [ ] **T20 判分纯逻辑 AnswerMatcher**
  - Acceptance：单选/判断全对或 0；多选集合完全相等满分、漏选/错选 0（无半分）；填空归一化（trim/全角转半角/忽略大小写/多答案 `||`）后命中满分否则 0；未答 0 且 answered=false
  - Verify：AnswerMatcherTest 穷举各题型对/错/未答/归一化边界，全绿（零 Spring 依赖）
  - Files：service/AnswerMatcher.java（+MatchResult）、src/test/.../AnswerMatcherTest.java

- [ ] **T21 判分明细与成绩实体**
  - Acceptance：attempt_answer（attempt_id,paper_question_id,type,student_answer,correct_answer,is_correct 0/1,score,full_score）与 exam_record（exam_id,student_id 唯一,total_score,best/last_attempt_id,attempt_count）建表
  - Verify：ddl-auto 建表；Repository 可用
  - Files：entity/AttemptAnswer.java、entity/ExamRecord.java、repository/AttemptAnswerRepository.java、repository/ExamRecordRepository.java

- [ ] **T22 判分服务与成绩聚合**
  - Acceptance：grade(attempt) 在同一事务内逐题判分、写 attempt_answer、更新 attempt=SUBMITTED、按 score_rule upsert exam_record（LAST 取末次/BEST 取最高并指向 attempt）；异常回滚无脏数据
  - Verify：GradingServiceTest 题数一致/总分正确/LAST 与 BEST 聚合/回滚无残留/未答 0 分 通过
  - Files：service/GradingService.java、src/test/.../GradingServiceTest.java

## M5 results — 成绩与错题

- [ ] **T23 学生成绩/回顾/错题本接口**
  - Acceptance：GET /api/student/results 成绩列表；/results/{examId}/review 交卷后返回题干/选项/我的答案/正确答案/对错/解析，未交卷 404，他人数据 403；/wrong-book 聚合 is_correct=0 题目可按课程筛选
  - Verify：ResultServiceTest 本人隔离/未交卷 404/错题聚合 通过
  - Files：service/ResultService.java、controller/StudentResultController.java、dto/ExamReviewDTO.java、src/test/.../ResultServiceTest.java

- [ ] **T24 教师成绩与统计接口**
  - Acceptance：records 按班级筛选成绩（含违规次数）；stats 返回参考人数/均分/最高最低/及格率(≥60)/分数段；空集合零值不报错；教师仅限本课程考试
  - Verify：ExamStatsServiceTest summarize() 正常/边界(60 及格、100 落[90-100])/空集合 通过
  - Files：service/ExamStatsService.java、controller/TeacherStatsController.java、dto/ExamStatsDTO.java、src/test/.../ExamStatsServiceTest.java

- [ ] **T25 前端成绩与统计页**
  - Acceptance：MyScores 成绩列表；ExamReview 对错配色+解析；WrongBook 错题本按课程筛选；ExamStats 教师统计表格/分数段展示
  - Verify：vue-tsc 零错误；手测学生回顾与教师统计
  - Files：frontend/src/api/result.ts、frontend/src/views/student/MyScores.vue、frontend/src/views/student/ExamReview.vue、frontend/src/views/student/WrongBook.vue、frontend/src/views/teacher/ExamStats.vue

## M6 收尾

- [ ] **T26 角色菜单整合与种子数据**
  - Acceptance：侧边栏按角色显示 题库/试卷/考试/成绩（教师）与 我的考试/成绩/错题本（学生）；提供种子数据（1 管理员、1 教师、若干学生账号、示例课程题库）便于演示
  - Verify：三角色登录菜单正确；种子数据启动后可登录
  - Files：frontend/src/components/Sidebar.vue、backend/.../config/DataInitializer.java（或 data/test-data.sql）

- [ ] **T27 端到端走查与 CI 确认**
  - Acceptance：完整链路走通（教师录题→组卷→发布闭卷+开卷各一场→学生选课→开考→倒计时交卷→自动出分→回顾/错题本→教师统计）；闭卷答题时网络响应无答案；CI 三作业（mvn test / frontend build / lint）配置有效
  - Verify：浏览器端到端手测 + `mvn test` 全绿 + `vue-tsc --noEmit` 零错误；更新变更单 CC-20260907-003 验证记录并置 reviewing
  - Files：无新增代码（缺陷修复按需）
