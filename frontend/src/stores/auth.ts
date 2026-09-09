/**
 * 认证状态 store（SPEC-identity）
 * 轻量响应式单例（不引入 Pinia），token 持久化到 localStorage。
 */
import { reactive, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { Role, UserInfo } from '@/api/auth'

const TOKEN_KEY = 'exam_sys_token'

const state = reactive<{ token: string; user: UserInfo | null }>({
  token: localStorage.getItem(TOKEN_KEY) ?? '',
  user: null
})

export function useAuth() {
  const isLoggedIn = computed(() => Boolean(state.token))
  const currentUser = computed(() => state.user)

  /** 登录并持久化 token */
  async function login(username: string, password: string): Promise<UserInfo> {
    const result = await authApi.login(username, password)
    state.token = result.token
    state.user = result.user
    localStorage.setItem(TOKEN_KEY, result.token)
    return result.user
  }

  /** 刷新当前用户信息（路由守卫首次进入时调用） */
  async function fetchMe(): Promise<UserInfo | null> {
    if (!state.token) {
      return null
    }
    try {
      state.user = await authApi.me()
      return state.user
    } catch {
      logout()
      return null
    }
  }

  /** 退出登录 */
  function logout(): void {
    state.token = ''
    state.user = null
    localStorage.removeItem(TOKEN_KEY)
  }

  /** 角色判断 */
  function hasRole(...roles: Role[]): boolean {
    return state.user !== null && roles.includes(state.user.role)
  }

  return { state, isLoggedIn, currentUser, login, logout, fetchMe, hasRole }
}
