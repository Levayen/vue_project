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
    <div class="bg-grid"></div>
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
  position: relative;
  background: #0a0e17;
}

.bg-grid {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(rgba(0, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
  z-index: 0;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  position: relative;
  z-index: 1;
}
</style>
