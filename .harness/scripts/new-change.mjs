#!/usr/bin/env node
// new-change.mjs — 状态驱动变更流：从模板生成变更单（proposed）
// 用法: node .harness/scripts/new-change.mjs "<变更标题>"
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const harnessRoot = dirname(dirname(fileURLToPath(import.meta.url)))
const title = process.argv[2]
if (!title) {
  console.error('用法: node .harness/scripts/new-change.mjs "<变更标题>"')
  process.exit(2)
}

const pad = (n) => String(n).padStart(2, '0')
const now = new Date()
const dateStr = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`
const today = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`

const changesDir = join(harnessRoot, 'changes')
const prefix = `CC-${dateStr}-`
const seq = readdirSync(changesDir).filter((f) => f.startsWith(prefix)).length + 1
const id = `${prefix}${String(seq).padStart(3, '0')}`
const file = join(changesDir, `${id}.md`)
if (existsSync(file)) {
  console.error(`[harness] 变更单已存在: ${file}`)
  process.exit(1)
}

const tpl = readFileSync(join(harnessRoot, 'workflow', 'change-template.md'), 'utf8')
const content = tpl
  .replace('<标题>', title)
  .replace('CC-YYYYMMDD-XXX', id)
  .replace('YYYY-MM-DD', today)
writeFileSync(file, content, 'utf8')

console.log(`[harness] 变更单已创建: ${file}`)
console.log('[harness] Planner 填写验收标准/技术方案/测试策略 → Developer 接单后改状态 in-progress')
