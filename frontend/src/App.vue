<script setup lang="ts">
/**
 * 应用根组件
 * 负责整体布局，包含侧边栏导航和主内容区域
 */
import { RouterView } from 'vue-router'
import Sidebar from '@/components/Sidebar.vue'
</script>

<template>
  <div class="app-container">
    <!-- 侧边栏导航组件 -->
    <Sidebar />
    <!-- 主内容区域，显示路由匹配的页面组件 -->
    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
}

.main-content {
  flex: 1;
  padding: 24px;
  background-color: #f5f7fa;
  overflow-y: auto;
}
</style>