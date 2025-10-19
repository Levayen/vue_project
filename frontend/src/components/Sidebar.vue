<script setup lang="ts">
/**
 * 侧边栏导航组件
 * 显示系统菜单和导航链接
 */
import { useRoute } from 'vue-router'

/**
 * 当前路由信息
 */
const route = useRoute()

/**
 * 菜单配置项
 * 包含菜单项名称、路径和图标
 */
const menuItems = [
  { name: '学生管理', path: '/students', icon: 'User' },
  { name: '班级管理', path: '/classes', icon: 'Building' },
  { name: '课程管理', path: '/courses', icon: 'BookOpen' },
  { name: '选课管理', path: '/enrollments', icon: 'GraduationCap' }
]

/**
 * 判断当前路径是否为活动状态
 * @param path 菜单路径
 * @return 是否为活动状态
 */
const isActive = (path: string) => route.path === path
</script>

<template>
  <aside class="sidebar">
    <div class="logo">
      <span class="logo-icon">📚</span>
      <span>学生管理系统</span>
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
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 24px;
}

.menu {
  flex: 1;
  padding: 16px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.menu-item.active {
  background: #4ecdc4;
  color: #1a1a2e;
}
</style>