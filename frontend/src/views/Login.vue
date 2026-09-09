<script setup lang="ts">
/**
 * 登录页（SPEC-identity）
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '@/stores/auth'
import type { Role } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()

const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)

const roleHome: Record<Role, string> = {
  ADMIN: '/students',
  TEACHER: '/students',
  STUDENT: '/student'
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await login(form.username.trim(), form.password)
    ElMessage.success(`欢迎，${user.username}`)
    const redirect = route.query.redirect as string | undefined
    await router.push(redirect && routeAvailableFor(redirect, user.role) ? redirect : roleHome[user.role])
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

/** 管理端路径仅教师/管理员可进入，避免重定向到越权页 */
function routeAvailableFor(path: string, role: Role): boolean {
  if (path.startsWith('/student')) {
    return role === 'STUDENT'
  }
  return role === 'ADMIN' || role === 'TEACHER'
}
</script>

<template>
  <div class="login-page">
    <div class="bg-grid"></div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">⬡</div>
        <h1 class="title">NEXUS</h1>
        <p class="subtitle">学生考试与管理系统</p>
      </div>

      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名（学生为学号）"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>

      <div class="login-tips">
        <p>演示账号：admin / admin123（管理员）</p>
        <p>teacher / teacher123（教师）· 学号 / student123（学生）</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0e17;
  position: relative;
  overflow: hidden;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

.login-card {
  width: 380px;
  padding: 40px 36px 28px;
  background: linear-gradient(180deg, rgba(13, 17, 23, 0.95), rgba(10, 22, 40, 0.95));
  border: 1px solid rgba(0, 255, 255, 0.2);
  border-radius: 12px;
  box-shadow: 0 0 40px rgba(0, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  font-size: 40px;
  color: #00ffff;
  text-shadow: 0 0 14px rgba(0, 255, 255, 0.6);
}

.title {
  font-family: 'Orbitron', sans-serif;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 6px;
  margin: 8px 0 4px;
  background: linear-gradient(90deg, #00ffff, #7b68ee);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: rgba(0, 255, 255, 0.5);
  font-size: 13px;
  letter-spacing: 2px;
  margin: 0;
}

.login-btn {
  width: 100%;
  letter-spacing: 4px;
}

.login-tips {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px solid rgba(0, 255, 255, 0.1);
  text-align: center;
}

.login-tips p {
  margin: 4px 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
}
</style>
