<script setup lang="ts">
/**
 * 学生端入口占位页（M0）
 * 考试/成绩/错题本等模块在后续里程碑（M3/M5）接入。
 */
import { useAuth } from '@/stores/auth'

const { currentUser } = useAuth()
</script>

<template>
  <div class="portal">
    <el-card class="portal-card">
      <h2>你好，{{ currentUser?.username }}</h2>
      <p>学生考试端将在后续版本开放，包括：在线考试、成绩查询、错题本。</p>
      <el-tag type="info">功能开发中</el-tag>
    </el-card>
  </div>
</template>

<style scoped>
.portal {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.portal-card {
  width: 520px;
  text-align: center;
}

.portal-card h2 {
  color: #00ffff;
  margin-top: 0;
}

.portal-card p {
  color: rgba(255, 255, 255, 0.6);
}
</style>
