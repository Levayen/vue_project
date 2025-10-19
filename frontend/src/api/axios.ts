/**
 * Axios 实例配置
 * 统一配置请求基础路径和超时时间
 */
import axios from 'axios'

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
 * 响应拦截器
 * 处理响应数据和错误
 */
instance.interceptors.response.use(
  response => response,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default instance