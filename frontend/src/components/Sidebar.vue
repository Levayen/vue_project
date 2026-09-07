<script setup lang="ts">
/**
 * 侧边栏导航组件
 * 显示系统菜单和导航链接
 */
import { useRoute } from 'vue-router'

const route = useRoute()

const menuItems = [
  { name: '学生管理', path: '/students', icon: 'User' },
  { name: '班级管理', path: '/classes', icon: 'Building' },
  { name: '课程管理', path: '/courses', icon: 'BookOpen' },
  { name: '选课管理', path: '/enrollments', icon: 'GraduationCap' }
]

const isActive = (path: string) => route.path === path
</script>

<template>
  <aside class="sidebar">
    <div class="logo">
      <div class="logo-glow"></div>
      <span class="logo-icon">⬡</span>
      <span class="logo-text">NEXUS</span>
      <span class="logo-sub">STUDENT SYS</span>
    </div>
    <nav class="menu">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        :class="['menu-item', { active: isActive(item.path) }]"
      >
        <el-icon :size="18">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.name }}</span>
        <div class="menu-item-glow" v-if="isActive(item.path)"></div>
      </router-link>
    </nav>
    <div class="sidebar-footer">
      <div class="status-line"></div>
      <span class="status-text">SYSTEM ONLINE</span>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #0d1117 0%, #0a1628 50%, #0d1117 100%);
  border-right: 1px solid rgba(0, 255, 255, 0.1);
  color: #fff;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: linear-gradient(180deg, transparent, rgba(0, 255, 255, 0.4), transparent);
}

.logo {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(0, 255, 255, 0.1);
  position: relative;
}

.logo-glow {
  position: absolute;
  top: 50%;
  left: 20px;
  width: 40px;
  height: 40px;
  background: radial-gradient(circle, rgba(0, 255, 255, 0.15), transparent);
  transform: translateY(-50%);
  border-radius: 50%;
}

.logo-icon {
  font-size: 28px;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.logo-text {
  font-family: 'Orbitron', sans-serif;
  font-size: 20px;
  font-weight: 900;
  background: linear-gradient(90deg, #00ffff, #7b68ee);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 3px;
}

.logo-sub {
  font-size: 10px;
  color: rgba(0, 255, 255, 0.4);
  letter-spacing: 2px;
  margin-left: auto;
}

.menu {
  flex: 1;
  padding: 20px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  color: rgba(255, 255, 255, 0.5);
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
  font-size: 15px;
  letter-spacing: 1px;
}

.menu-item:hover {
  color: rgba(0, 255, 255, 0.8);
  background: rgba(0, 255, 255, 0.05);
}

.menu-item.active {
  color: #00ffff;
  background: rgba(0, 255, 255, 0.08);
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: #00ffff;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.6);
  border-radius: 0 2px 2px 0;
}

.menu-item-glow {
  position: absolute;
  right: 20px;
  width: 6px;
  height: 6px;
  background: #00ffff;
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(0, 255, 255, 0.8);
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(0, 255, 255, 0.1);
  position: relative;
}

.status-line {
  position: absolute;
  top: 0;
  left: 20px;
  right: 20px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.3), transparent);
}

.status-text {
  font-family: 'Orbitron', sans-serif;
  font-size: 10px;
  color: rgba(0, 255, 255, 0.4);
  letter-spacing: 3px;
}
</style>
