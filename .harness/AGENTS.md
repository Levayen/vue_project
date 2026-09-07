# AGENTS.md — Harness 工程入口约束

> 本文件是所有 AI 代理 / 开发者在本仓库工作的**唯一入口**。动手前必须读完本文件与
> `harness.yaml`。规则的机读版本以 `harness.yaml` 为准；人工解释以本文件为准。

## 1. 铁律（不可违反）

1. **先变更单，后写码**：任何非琐碎改动（>3 行 / 跨文件 / 新功能 / 修 bug）必须先运行
   `node .harness/scripts/new-change.mjs "<标题>"` 生成变更单，状态流转为
   `proposed → in-progress → reviewing → done`。
2. **规约先行**：后端代码遵守 `rules/backend-java-alibaba.md`（阿里巴巴 Java 开发手册），
   前端代码遵守 `rules/frontend-vue-alibaba.md`（阿里前端规范 + Vue3 风格指南）。
3. **质量门禁不可绕过**：`pre-commit` 与 `commit-msg` Hook 必须通过；禁止
   `--no-verify` 提交。本地缺失工具链导致的跳过（WARN）必须在变更单中记录。
4. **提交信息**：遵守 `rules/git-commit.md` 的 Conventional Commits 规范。
5. **最小改动**：不主动创建与变更单无关的文件；不重构无关代码；不主动新建文档文件
   （`.harness/` 内规则文件除外）。
6. **禁止硬编码**：密钥、URL、魔法值入配置；数据库密码等敏感项不进代码库。

## 2. 全流程工作流（状态驱动）

```
需求/问题 ──▶ [Planner]  ──▶ 变更单(proposed) ──▶ [Developer] ──▶ 变更单(in-progress)
                                      │                                    │
                                      ▼                                    ▼
                          验收标准 + 影响面 + 测试策略          本地门禁(gate.mjs) + 小步提交
                                      │                                    │
                                      └──────────────▶ [Reviewer] ◀─────────┘
                                                        │ 按 rules 清单逐条评审
                                                        ▼
                                              变更单(reviewing) → CI 全绿 → done
```

| 阶段 | 角色 | 输入 | 产出 |
|------|------|------|------|
| 1 需求 | Planner | 用户需求 | 澄清后的问题陈述 |
| 2 规格 | Planner | 问题陈述 | 变更单（验收标准/影响面/测试策略） |
| 3 开发 | Developer | 变更单(in-progress) | 代码 + 测试，本地门禁通过 |
| 4 验证 | Developer | 代码 | 单测/手测证据写入变更单 |
| 5 评审 | Reviewer | PR/diff | 按 Reviewer 清单出结论（pass/block） |
| 6 合入 | Reviewer | CI + 评审结论 | 变更单(done) |

角色职责详见 `agents/planner.md`、`agents/developer.md`、`agents/reviewer.md`。

## 3. 技术栈与门禁（详见 harness.yaml）

| 栈 | 路径 | 本地门禁（pre-commit 按改动路由） | CI 门禁 |
|----|------|-----------------------------------|---------|
| Spring Boot 后端 | `backend/` | `mvnw -q -DskipTests compile` | `mvn -q test` |
| Vue3+TS 管理端 | `frontend/` | `vue-tsc --noEmit` 类型检查 | `npm run build` |
| Vue3 象棋 Web | 根目录 | `npm run lint`（依赖缺失时 WARN 跳过） | `npm run lint` |

## 4. 常用命令

```powershell
# 环境自检（--fix 可自动设置 git hooksPath）
node .harness/scripts/harness-doctor.mjs --fix

# 新建变更单
node .harness/scripts/new-change.mjs "修复编辑弹窗被遮挡"

# 手动跑本地门禁（按暂存区改动自动路由）
node .harness/scripts/gate.mjs staged

# 手动校验提交信息
node .harness/scripts/gate.mjs commit-msg <message-file>
```

## 5. 评审阻断项（Reviewer 一票否决）

- 违反阿里 Java 手册"强制"级别条目（异常吞掉、`Executors` 建线程池、`select *`、
  POJO 缺 `toString`、魔法值等）。
- 前端 Vue3 风格指南 Priority A 违规（组件名单词、`v-for` 缺 `key`、`v-if`+`v-for` 同级等）。
- 无变更单、提交信息不合规、CI 红灯。
- 引入新依赖未在变更单说明理由。
