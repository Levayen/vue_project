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
- [ ] 分层清晰：Controller 不写业务、Repository 不返回 DTO

### 前端（对照 rules/frontend-vue-alibaba.md）
- [ ] 组件名多词且 PascalCase；`v-for` 有稳定 `key`；无 `v-if`+`v-for` 同级
- [ ] props 声明类型；组件间通信用 props/emits，无直接改 prop
- [ ] `<script setup lang="ts">`；composables 以 `use` 开头
- [ ] 请求走 `src/api` 层；无硬编码 baseURL
- [ ] 样式 `scoped`；无 `!important`（除非变更单说明）
- [ ] 弹窗/浮层注意 teleport（append-to-body），不受父容器 overflow/transform 裁剪

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
