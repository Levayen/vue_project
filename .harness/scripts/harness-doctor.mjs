#!/usr/bin/env node
// harness-doctor.mjs — Harness 环境自检；--fix 自动修复可修复项（git hooksPath）
import { execSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const harnessRoot = dirname(dirname(fileURLToPath(import.meta.url)))
const repoRoot = dirname(harnessRoot)
const fix = process.argv.includes('--fix')
let failures = 0
let warns = 0

const ok = (m) => console.log(`  PASS  ${m}`)
const bad = (m) => { console.log(`  FAIL  ${m}`); failures++ }
const warn = (m) => { console.log(`  WARN  ${m}`); warns++ }

function tryExec(cmd) {
  try {
    return execSync(cmd, { cwd: repoRoot, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim()
  } catch {
    return null
  }
}

console.log('== Harness Doctor ==')

console.log('[1] 骨架文件完整性')
const required = [
  'AGENTS.md', 'harness.yaml',
  'agents/planner.md', 'agents/developer.md', 'agents/reviewer.md',
  'rules/backend-java-alibaba.md', 'rules/frontend-vue-alibaba.md', 'rules/git-commit.md',
  'workflow/change-template.md',
  'scripts/new-change.mjs', 'scripts/gate.mjs', 'scripts/harness-doctor.mjs',
  'hooks/pre-commit', 'hooks/commit-msg',
]
for (const f of required) {
  if (existsSync(join(harnessRoot, f))) ok(f)
  else bad(`缺失文件: .harness/${f}`)
}

console.log('[2] Git 与本地守门')
if (tryExec('git rev-parse --is-inside-work-tree')) ok('git 仓库')
else bad('当前目录不是 git 仓库')

let hooksPath = null
try {
  hooksPath = execSync('git config core.hooksPath', { cwd: repoRoot, encoding: 'utf8' }).trim()
} catch { /* 未配置 */ }
if (hooksPath === '.harness/hooks') {
  ok('core.hooksPath = .harness/hooks（本地守门已激活）')
} else if (fix) {
  execSync('git config core.hooksPath .harness/hooks', { cwd: repoRoot })
  ok('已自动设置 core.hooksPath = .harness/hooks')
} else {
  bad(`core.hooksPath 未指向 .harness/hooks（当前: "${hooksPath || '未设置'}"）；运行 node .harness/scripts/harness-doctor.mjs --fix 修复`)
}

console.log('[3] 工具链')
ok(`node ${process.version}`)
const java = tryExec('java -version 2>&1')
if (java) ok(java.split('\n')[0])
else warn('未检测到 java（后端门禁需要 JDK 21+）')
const npm = tryExec('npm -v')
if (npm) ok(`npm ${npm}`)
else warn('未检测到 npm')

console.log('[4] 栈依赖')
if (existsSync(join(repoRoot, 'frontend', 'node_modules'))) ok('frontend/node_modules 已安装')
else warn('frontend/node_modules 缺失（前端门禁将跳过；在 frontend/ 执行 npm install）')
if (existsSync(join(repoRoot, 'node_modules'))) ok('根目录 node_modules 已安装')
else warn('根目录 node_modules 缺失（象棋工程 lint 将跳过；在仓库根执行 npm install）')
if (existsSync(join(repoRoot, 'backend', 'pom.xml'))) ok('backend/pom.xml 存在')
else warn('未发现 backend/pom.xml')

console.log('')
if (failures > 0) {
  console.log(`自检结果: ${failures} 个 FAIL, ${warns} 个 WARN — 请先修复 FAIL 项`)
  process.exit(1)
}
console.log(`自检结果: 全部通过（${warns} 个 WARN 为可选依赖提示）`)
