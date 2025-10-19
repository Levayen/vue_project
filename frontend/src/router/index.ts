/**
 * 路由配置
 * 定义系统的页面路由映射关系
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Students from '@/views/Students.vue'
import Classes from '@/views/Classes.vue'
import Courses from '@/views/Courses.vue'
import Enrollments from '@/views/Enrollments.vue'

/**
 * 路由记录数组
 * 定义各页面的路径、名称和组件
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/students'
  },
  {
    path: '/students',
    name: 'Students',
    component: Students
  },
  {
    path: '/classes',
    name: 'Classes',
    component: Classes
  },
  {
    path: '/courses',
    name: 'Courses',
    component: Courses
  },
  {
    path: '/enrollments',
    name: 'Enrollments',
    component: Enrollments
  }
]

/**
 * 创建路由实例
 * 使用 HTML5 History 模式
 */
const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router