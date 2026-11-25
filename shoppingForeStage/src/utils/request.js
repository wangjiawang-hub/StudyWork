/**
 * 全局 Axios 封装：统一注入 JWT、处理业务错误与 401 登出。
 * 开发环境由 vite.config.js 代理到 Gateway :8080。
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

/** 公开接口：401 时不提示「登录过期」，避免未登录浏览商品时误报 */
const PUBLIC_PREFIXES = ['/auth/login', '/auth/admin/login', '/auth/register', '/auth/captcha', '/auth/sendCode', '/goods/', '/user/review/list', '/ai/chat']

function isPublicRequest(url = '') {
  return PUBLIC_PREFIXES.some(prefix => url.startsWith(prefix))
}

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

/** 请求拦截：从 Pinia 读取 token，写入 Authorization 供网关 AuthFilter 校验 */
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  const token = userStore.token
  console.log('token:', token, 'localStorage:', localStorage.getItem('user'))
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

/**
 * 响应拦截：后端统一返回 { success, data, message }。
 * 成功时直接返回该对象，页面用 res.data 取业务数据。
 */
request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.success === false) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  err => {
    const status = err.response?.status
    const url = err.config?.url || ''
    const msg = typeof err.response?.data === 'string'
      ? err.response.data
      : err.response?.data?.message

    if (status === 401) {
      const userStore = useUserStore()
      if (userStore.token) {
        userStore.logout()
      }
      if (!isPublicRequest(url)) {
        ElMessage.error(msg || '登录已过期，请重新登录')
      }
    } else {
      ElMessage.error(msg || '网络错误')
    }
    return Promise.reject(err)
  }
)

export default request
