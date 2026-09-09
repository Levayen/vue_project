/**
 * 认证相关接口（SPEC-identity）
 */
import request from './axios'

export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface UserInfo {
  id: number
  username: string
  role: Role
  studentId: number | null
  enabled: boolean
  /** 种子账号首次登录强制改密标记 */
  mustChangePassword?: boolean
}

export interface LoginResult {
  token: string
  user: UserInfo
}

/** 登录 */
export function login(username: string, password: string): Promise<LoginResult> {
  return request.post('/auth/login', { username, password }).then(res => res.data)
}

/** 获取当前登录用户 */
export function me(): Promise<UserInfo> {
  return request.get('/auth/me').then(res => res.data)
}

/** 修改密码（首次登录强制改密 / 自助改密） */
export function changePassword(oldPassword: string, newPassword: string): Promise<UserInfo> {
  return request.post('/auth/change-password', { oldPassword, newPassword }).then(res => res.data)
}
