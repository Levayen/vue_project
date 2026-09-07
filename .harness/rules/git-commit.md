# Git 提交规范 — Conventional Commits

## 格式

```
<type>(<scope>): <subject>

<body 可选：说明动机与改动要点>

<footer 可选：BREAKING CHANGE / 关联变更单>
```

## type 取值

| type | 用途 |
|------|------|
| feat | 新功能 |
| fix | 修复缺陷 |
| docs | 文档（仅 .harness 规则/说明类） |
| style | 格式调整，不影响逻辑（空格/分号/格式） |
| refactor | 重构，非新功能非修复 |
| perf | 性能优化 |
| test | 测试相关 |
| build | 构建系统/依赖（pom.xml、package.json、vite） |
| ci | CI 配置与脚本 |
| chore | 杂项（不影响源码/测试） |
| revert | 回滚提交 |

## 规则
- 【强制】subject 简明描述"做了什么"，≤72 字符，结尾不加句号。
- 【强制】scope 用栈名或模块名：`backend`、`frontend`、`students`、`harness` 等。
- 【强制】一个提交只做一件事；修复类提交关联变更单，如 footer 写 `Harness-Change: CC-xxx`。
- 【强制】禁止 `--no-verify` 绕过 Hook；Hook 误报时修复 Hook 而非跳过。

## 示例

```
feat(students): 学生列表支持按姓名学号搜索
fix(frontend): 编辑弹窗 teleport 到 body 修复被容器裁剪
refactor(backend): 学生更新逻辑下沉到 service 层
ci(harness): 新增后端测试与前端构建质量漏斗
```
