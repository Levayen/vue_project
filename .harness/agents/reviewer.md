# 角色：Reviewer（评审者）

## 职责
对 `reviewing` 状态的变更做独立质量把关，结论只有 `pass` 或 `block`。

## 评审清单（逐条过，任何一条不过即 block）

### 流程
- [ ] 变更单存在且状态为 `reviewing`，验收标准可验证
- [ ] 实际 diff 与变更单方案一致，无越界改动、无无关文件
- [ ] 提交信息符合 Conventional Commits，无 `--no-verify` 痕迹

### 后端（对照 rules/backend-java-alibaba.md）
- [ ] 命名合规：类 UpperCamelCase、方法/变量 lowerCamelCase、常量全大写下划线
- [ ] 无魔法值；long 字面量用大写 `L`
- [ ] 包装类比较用 `equals`；POJO 有 `toString`；`equals` 常量在前
- [ ] 无空 catch；finally 中无 return；异常有上下文日志
- [ ] 线程池用 `ThreadPoolExecutor` 显式创建，无 `Executors.newXxx`
- [ ] 控制语句大括号完整；三目运算无空指针陷阱
- [ ] SQL/ORM：无 `select *`；表字段小写下划线；必含 create_time/update_time
- [ ] IDENTITY 主键主从实体：主实体先 save 再构建子实体关联，无 TransientPropertyValueException 风险
- [ ] JPA 派生查询命名：普通列用驼峰（`existsByQuestionId`），下划线仅用于关联路径（`existsByQuestion_Id`）
- [ ] 批量写入（组卷/判分）先全量校验再统一落库，无逐条保存
- [ ] 字符串归一化函数末尾 trim 收尾；时间窗口业务以服务端时间为准
- [ ] 模块间 Port 解耦：默认桩用 `@Component`（无 `@ConditionalOnMissingBean`），真实实现 `@Primary`
- [ ] text 字段 JSON 解析有 try-catch 容错，单条脏数据不导致接口 500
- [ ] Service/组件构造函数（或依赖注入）新增依赖时，同步更新所有 `new XxxService(...)` 测试调用点，`mvnw test-compile` 通过
- [ ] 统计/聚合类 DTO 字段有注释说明统计口径（总数/当前进行中数/累计数），避免字段名歧义
- [ ] 分层清晰：Controller 不写业务、Repository 不返回 DTO

### 前端（对照 rules/frontend-vue-alibaba.md）
- [ ] 组件名多词且 PascalCase；`v-for` 有稳定 `key`；无 `v-if`+`v-for` 同级
- [ ] props 声明类型；组件间通信用 props/emits，无直接改 prop
- [ ] `<script setup lang="ts">`；composables 以 `use` 开头
- [ ] 请求走 `src/api` 层；无硬编码 baseURL
- [ ] `src/api/*.ts` interface 字段与后端 DTO 完全一致，无遗漏字段
- [ ] 选项答案编码转文本用 `match(/[A-Za-z]/g)`，无 `split('')` 或错误 split
- [ ] 样式 `scoped`；无 `!important`（除非变更单说明）
- [ ] 弹窗/浮层注意 teleport（append-to-body），不受父容器 overflow/transform 裁剪
- [ ] `setInterval`/`setTimeout` 句柄在 `onBeforeUnmount` 清理；弹窗轮询定时器在 dialog `@closed` 清理；start 前先 stop 防重复

### 接口对接易错字段（本项目特定）
- [ ] `GET /api/questions` 返回 Page 对象，前端用 `.content` 取数组
- [ ] `GET /api/papers/{id}` 题目列表字段为 `items`（非 `questions`）
- [ ] `POST /api/papers/random` 规则分数字段为 `scorePerQuestion`（非 `score`）
- [ ] `POST /api/admin/users/{id}/reset-password` 请求体为 `{ password }`（非 `{ newPassword }`）
- [ ] `POST /api/exams` 时间字段格式 `yyyy-MM-ddTHH:mm:ss`（不带 Z）

### 安全与性能
- [ ] 无密钥/密码硬编码入库；用户输入有校验（后端 `@Valid`）
- [ ] 无明显 N+1 查询、无大对象全量加载
- [ ] 新依赖已在变更单说明用途与替代方案比较

## 输出格式
```
结论：pass / block
阻断项：<文件:行号 — 违反规则条目 — 修复建议>
建议项：<非阻断的改进>
```
block 后 Developer 修复重提，Reviewer 复审；pass 且 CI 全绿后状态置 `done`。
