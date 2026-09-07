#!/usr/bin/env node
// gate.mjs — 本地质量门禁（零依赖，Node >= 18）
//   node gate.mjs staged            按暂存区改动路由到各栈本地门禁
//   node gate.mjs commit-msg <file> 校验提交信息（Conventional Commits）
// 路由规则与 harness.yaml stacks 保持一致
import { execSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const harnessRoot = dirname(dirname(fileURLToPath(import.meta.url)))
const repoRoot = dirname(harnessRoot)
const isWin = process.platform === 'win32'

const log = {
  info: (m) => console.log(`[gate] ${m}`),
  pass: (m) => console.log(`[gate] PASS  ${m}`),
  warn: (m) => console.warn(`[gate] WARN  ${m}`),
  fail: (m) => console.error(`[gate] FAIL  ${m}`),
}

function run(cmd, cwd) {
  execSync(cmd, { cwd, stdio: 'inherit', timeout: 300000, shell: isWin ? 'cmd.exe' : '/bin/sh' })
}
function git(args) {
  return execSync(`git ${args}`, { cwd: repoRoot, encoding: 'utf8' }).trim()
}

// 后端优先使用工程内置 Maven（tools/maven），避免 wrapper 联网下载
const mavenCmd = isWin
  ? 'tools\\maven\\apache-maven-3.9.16\\bin\\mvn.cmd -q -DskipTests compile'
  : 'mvn -q -DskipTests compile'

const stacks = [
  {
    id: 'backend',
    match: [/^backend\/.*\.java$/, /^backend\/pom\.xml$/],
    gate: () => run(mavenCmd, join(repoRoot, 'backend')),
  },
  {
    id: 'frontend-admin',
    match: [/^frontend\/.*\.ts$/, /^frontend\/.*\.vue$/, /^frontend\/package\.json$/],
    gate: () => {
      if (!existsSync(join(repoRoot, 'frontend', 'node_modules'))) {
        log.warn('frontend/node_modules 不存在，跳过类型检查（请先在 frontend/ 执行 npm install）')
        return
      }
      run('npx --no-install vue-tsc --noEmit', join(repoRoot, 'frontend'))
    },
  },
  {
    id: 'web-chess',
    match: [/^src\/.*\.js$/, /^src\/.*\.vue$/, /^package\.json$/],
    gate: () => {
      if (!existsSync(join(repoRoot, 'node_modules'))) {
        log.warn('根目录 node_modules 不存在，跳过 lint（请先在仓库根执行 npm install）')
        return
      }
      run('npm run lint', repoRoot)
    },
  },
]

function staged() {
  let files = []
  try {
    files = git('diff --cached --name-only --diff-filter=ACMR')
      .split('\n')
      .filter(Boolean)
      .map((f) => f.replace(/\\/g, '/'))
  } catch {
    log.fail('无法读取 git 暂存区，请确认在 git 仓库内执行')
    process.exit(1)
  }
  if (files.length === 0) {
    log.info('暂存区无改动，跳过门禁')
    return
  }
  log.info(`暂存 ${files.length} 个文件，开始路由本地门禁...`)
  const affected = stacks.filter((s) => files.some((f) => s.match.some((re) => re.test(f))))
  if (affected.length === 0) {
    log.pass('改动不涉及受规约栈，门禁通过')
    return
  }
  let failed = false
  for (const s of affected) {
    log.info(`运行 [${s.id}] 本地门禁...`)
    try {
      s.gate()
      log.pass(`[${s.id}] 门禁通过`)
    } catch {
      failed = true
      log.fail(`[${s.id}] 门禁失败`)
    }
  }
  if (failed) {
    log.fail('本地门禁未通过，提交已阻断。请修复后重新提交（禁止使用 --no-verify）')
    process.exit(1)
  }
  log.pass('全部本地门禁通过')
}

const MSG_RE = /^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([^)]+\))?: .+/
function commitMsg(file) {
  const raw = readFileSync(file, 'utf8')
  const subject = (raw.split(/\r?\n/).find((l) => l.trim() && !l.startsWith('#')) || '').trim()
  if (subject.startsWith('Merge')) {
    log.pass('merge 提交，跳过校验')
    return
  }
  if (!MSG_RE.test(subject)) {
    log.fail('提交信息不符合 Conventional Commits 规范')
    log.fail('格式: <type>(<scope>): <subject>')
    log.fail('type: feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert')
    log.fail(`当前: "${subject}"`)
    process.exit(1)
  }
  if (subject.length > 72) {
    log.fail(`subject 长度 ${subject.length} 超过 72 字符上限`)
    process.exit(1)
  }
  log.pass('提交信息校验通过')
}

const [, , sub, arg] = process.argv
if (sub === 'staged') staged()
else if (sub === 'commit-msg') {
  if (!arg) {
    log.fail('用法: node gate.mjs commit-msg <message-file>')
    process.exit(2)
  }
  commitMsg(arg)
} else {
  log.fail('用法: node gate.mjs <staged|commit-msg <file>>')
  process.exit(2)
}
