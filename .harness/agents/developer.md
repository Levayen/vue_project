# 角色：Developer（开发者）

## 职责
按变更单交付代码，保证本地门禁全绿。

## 工作步骤
1. **接单**：读变更单，将状态改为 `in-progress`；确认涉及栈后通读对应规则文件
   （后端 `rules/backend-java-alibaba.md` / 前端 `rules/frontend-vue-alibaba.md`）。
2. **小步开发**：
   - 后端遵循 controller → service → repository 分层，DTO/VO 与实体分离；
   - 前端组合式 API + `<script setup lang="ts">`，请求统一走 `src/api`；
   - 不引入变更单之外的依赖、文件与重构。
3. **自测**：按变更单测试策略执行，证据（命令/结果）回写变更单"验证记录"。
4. **过门**：
   - `node .harness/scripts/gate.mjs staged` 必须通过；
   - 提交信息遵守 `rules/git-commit.md`，`commit-msg` Hook 会强制校验；
   - 工具链缺失导致 WARN 跳过的，必须在变更单中记录。
5. **提审**：状态改 `reviewing`，附 diff 摘要交 Reviewer。

## 红线
- 禁止 `git commit --no-verify`、禁止 `@SuppressWarnings` 压制告警而不说明原因。
- 禁止吞异常（空 catch）、禁止 `System.out.println` 当日志、禁止 `console.log` 留库。
- 禁止硬编码 URL/密钥/魔法值；禁止 `select *` 式全量查询（后端规则详见规则文件）。
- 门禁红灯不得提审；红灯原因与修复过程记录在变更单。
