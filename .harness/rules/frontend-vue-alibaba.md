# 前端规则 — 阿里巴巴前端规范 + Vue3 官方风格指南

> 适用范围：`frontend/`（Vue3+TS+Vite+Element Plus）与根目录 Vue 工程。
> 标注【强制】的为评审阻断项；Vue 风格指南 Priority A/B 同等级对待。

## 1. 组件（Vue3 Style Guide Priority A/B）
- 【强制】组件名至少两个单词且 PascalCase：`StudentTable.vue`，禁止 `Student.vue`。
- 【强制】`v-for` 必须绑定稳定 `key`（用 id，不用 index 作为列表可变时的 key）。
- 【强制】禁止 `v-if` 与 `v-for` 同元素；需过滤用 computed。
- 【强制】props 必须声明类型（TS 接口 / `defineProps<{}>()`）；prop 名 camelCase 声明、kebab-case 传值。
- 【强制】组件样式必须 `scoped`；禁止直接修改 prop，变更通过 emit。
- 【推荐】单文件组件顺序：`<script setup>` → `<template>` → `<style scoped>`。

## 2. 组合式 API 与 TS
- 【强制】统一 `<script setup lang="ts">`；逻辑复用抽 composables，文件名/函数名 `useXxx`。
- 【强制】响应式优先 `ref`/`computed`；props/emit 用类型声明，禁止 `any` 泛滥（接口数据定义在 `src/api` 类型层）。
- 【推荐】组件 ≤200 行；超过则拆 composable/子组件。

## 3. 接口与数据
- 【强制】所有 HTTP 请求走 `src/api` 层（axios 实例 + 拦截器），组件内不直接写 axios/fetch。
- 【强制】禁止硬编码 baseURL；接口返回结构用 TS 类型标注。
- 【强制】用户输入必填项有校验（表单 rules）；删除等危险操作有二次确认。
- 【推荐】列表请求带 loading 与错误提示（ElMessage），失败不白屏。

## 4. 命名与目录
- 【强制】视图/页面放 `src/views/`，可复用组件放 `src/components/`；
  组件文件名 PascalCase，TS/JS 工具文件 kebab/camel 与项目现状一致。
- 【强制】变量/函数 lowerCamelCase，常量全大写；布尔变量 `is/has/can` 前缀。
- 【推荐】事件处理函数 `handleXxx`；触发事件 `@xxx` 用 kebab-case。

## 5. 样式与 UI
- 【强制】弹窗/抽屉/浮层类组件使用 `append-to-body`（teleport 到 body），
  避免父容器 `overflow:hidden`/`backdrop-filter`/`transform` 造成裁剪或层叠错误。
- 【强制】禁止 `!important`（特殊主题覆盖需变更单说明）；颜色/间距优先用主题变量。
- 【推荐】布局响应式；Element Plus 组件不深度覆盖样式，必要时用 `:deep()` 并注释原因。

## 6. 代码质量
- 【强制】禁止 `console.log/debugger` 提交；禁止注释掉的代码块入库。
- 【强制】`npm run build`（含 `vue-tsc`）必须零 TS 错误。
- 【推荐】ESLint + Prettier 统一格式；提交前本地格式化。
- 【推荐】路由懒加载；第三方大组件按需引入（Element Plus 自动导入）。

## 7. 安全
- 【强制】不用 `v-html` 渲染不可信内容（XSS）。
- 【强制】敏感信息（token）存放在约定存储位，不写死在代码；URL 参数做编码。
