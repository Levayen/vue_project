/**
 * 路由配置
 * 定义系统的页面路由映射关系；meta.roles 控制角色访问（SPEC-identity）
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Students from '@/views/Students.vue'
import Classes from '@/views/Classes.vue'
import Courses from '@/views/Courses.vue'
import Enrollments from '@/views/Enrollments.vue'
import Login from '@/views/Login.vue'
import QuestionBank from '@/views/teacher/QuestionBank.vue'
import PaperList from '@/views/teacher/PaperList.vue'
import PaperEdit from '@/views/teacher/PaperEdit.vue'
import ExamManage from '@/views/teacher/ExamManage.vue'
import ExamStats from '@/views/teacher/ExamStats.vue'
import ReviewCenter from '@/views/teacher/ReviewCenter.vue'
import ExamList from '@/views/student/ExamList.vue'
import ExamRoom from '@/views/student/ExamRoom.vue'
import MyScores from '@/views/student/MyScores.vue'
import ExamReview from '@/views/student/ExamReview.vue'
import WrongBook from '@/views/student/WrongBook.vue'
import { useAuth } from '@/stores/auth'
import type { Role } from '@/api/auth'

/**
 * 路由记录数组
 * meta.public: 无需登录；meta.roles: 允许访问的角色
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/students'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/student',
    redirect: '/student/exams'
  },
  {
    path: '/student/exams',
    name: 'ExamList',
    component: ExamList,
    meta: { roles: ['STUDENT'] as Role[] }
  },
  {
    path: '/student/exam/:examId',
    name: 'ExamRoom',
    component: ExamRoom,
    meta: { roles: ['STUDENT'] as Role[] }
  },
  {
    path: '/student/scores',
    name: 'MyScores',
    component: MyScores,
    meta: { roles: ['STUDENT'] as Role[] }
  },
  {
    path: '/student/scores/:examId/review',
    name: 'ExamReview',
    component: ExamReview,
    meta: { roles: ['STUDENT'] as Role[] }
  },
  {
    path: '/student/wrong-book',
    name: 'WrongBook',
    component: WrongBook,
    meta: { roles: ['STUDENT'] as Role[] }
  },
  {
    path: '/students',
    name: 'Students',
    component: Students,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/classes',
    name: 'Classes',
    component: Classes,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/courses',
    name: 'Courses',
    component: Courses,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/enrollments',
    name: 'Enrollments',
    component: Enrollments,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/questions',
    name: 'QuestionBank',
    component: QuestionBank,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/papers',
    name: 'PaperList',
    component: PaperList,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/papers/new',
    name: 'PaperCreate',
    component: PaperEdit,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/papers/:id/edit',
    name: 'PaperEditPage',
    component: PaperEdit,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/exams',
    name: 'ExamManage',
    component: ExamManage,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/exams/:examId/stats',
    name: 'ExamStats',
    component: ExamStats,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
  },
  {
    path: '/reviews',
    name: 'ReviewCenter',
    component: ReviewCenter,
    meta: { roles: ['ADMIN', 'TEACHER'] as Role[] }
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

/**
 * 全局前置守卫：未登录跳登录页；角色不足回落到其角色首页
 */
router.beforeEach(async to => {
  const { state, fetchMe } = useAuth()

  if (to.meta.public) {
    return true
  }

  if (!state.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 刷新页面后内存用户丢失，用 token 拉取当前用户
  if (!state.user) {
    const user = await fetchMe()
    if (!user) {
      return { path: '/login' }
    }
  }

  const allowedRoles = to.meta.roles as Role[] | undefined
  if (allowedRoles && !allowedRoles.includes(state.user!.role)) {
    return state.user!.role === 'STUDENT' ? '/student' : '/students'
  }

  return true
})

export default router
