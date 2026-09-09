<script setup lang="ts">
/**
 * 应用根组件
 * 负责整体布局，包含侧边栏导航和主内容区域；登录页等公共页全屏无侧边栏。
 * Issue1：种子账号首次登录强制修改密码——全局拦截弹窗，未改密前不可关闭。
 */
import { computed, reactive, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Sidebar from '@/components/Sidebar.vue'
import { useAuth } from '@/stores/auth'
import { changePassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const { state, logout, fetchMe } = useAuth()

/** 种子账号首次登录强制改密 */
const mustChange = computed(() => Boolean(state.token) && Boolean(state.user?.mustChangePassword))

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdSubmitting = ref(false)

function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

async function handleChangePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (pwdForm.newPassword === pwdForm.oldPassword) {
    ElMessage.warning('新密码不能与原密码相同')
    return
  }
  pwdSubmitting.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    await fetchMe()
    ElMessage.success('密码修改成功，欢迎进入系统')
    resetPwdForm()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '密码修改失败')
  } finally {
    pwdSubmitting.value = false
  }
}

/** 放弃改密则退出登录 */
function handleLogoutFromDialog() {
  logout()
  resetPwdForm()
  router.push('/login')
}
</script>

<template>
  <div class="app-container">
    <div class="bg-grid"></div>
    <!-- 侧边栏导航组件（公共页不显示） -->
    <Sidebar v-if="!route.meta.public" />
    <!-- 主内容区域，显示路由匹配的页面组件 -->
    <main class="main-content" :class="{ 'main-content--full': route.meta.public }">
      <RouterView />
    </main>

    <!-- 首次登录强制修改密码（不可关闭，改密前拦截所有操作） -->
    <el-dialog
      :model-value="mustChange"
      title="首次登录，请修改初始密码"
      width="420px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="系统检测到您使用的是初始密码，为保障账号安全，请先修改密码后再使用系统。"
        class="pwd-alert"
      />
      <el-form label-width="90px" class="pwd-form">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleLogoutFromDialog">退出登录</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
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

.main-content--full {
  padding: 0;
}

.pwd-alert {
  margin-bottom: 18px;
  text-align: left;
}
</style>
