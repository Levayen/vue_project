/**
 * Axios 实例配置
 * 统一配置请求基础路径、超时时间，以及认证拦截器（SPEC-identity）
 */
import axios from 'axios'

const TOKEN_KEY = 'exam_sys_token'

/**
 * 创建 Axios 实例
 * baseURL: '/api' - 请求会被代理到后端 API
 * timeout: 10000 - 请求超时时间为10秒
 */
const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/**
 * 请求拦截器：自动携带 Authorization 头
 */
instance.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 响应拦截器
 * 401（未登录/令牌失效）时清除 token 并跳转登录页；其余错误继续抛出
 */
instance.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem(TOKEN_KEY)
      window.location.href = '/login'
    }
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default instance
