/**
 * 用户登录态：JWT 与用户信息同步存 Pinia + localStorage，刷新页面不丢登录。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = computed(() => user.value?.token || '')

  async function login(data) {
    const res = await request.post('/auth/login', data)
    user.value = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    return res.data
  }

  // 邮箱验证码登录（免密码）
  async function loginByCode(email, code) {
    const res = await request.post('/auth/login/code', { email, code })
    user.value = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    return res.data
  }

  // 发送邮箱验证码
  async function sendEmailCode(email) {
    return request.post('/auth/sendCode', { target: email, type: 'email' })
  }

  async function adminLogin(data) {
    const res = await request.post('/auth/admin/login', data)
    user.value = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    return res.data
  }

  async function register(data) {
    return request.post('/auth/register', data)
  }

  function logout() {
    user.value = null
    localStorage.removeItem('user')
  }

  async function fetchProfile() {
    const res = await request.get('/auth/profile')
    user.value = { ...user.value, ...res.data, token: user.value.token }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  return { user, token, login, loginByCode, sendEmailCode, adminLogin, register, logout, fetchProfile }
})
