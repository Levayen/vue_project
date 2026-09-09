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
