<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useMallStore } from '../stores/mall'
import CaptchaField from '../components/CaptchaField.vue'
import { isValidPhone, isValidEmail, trimAuthFields } from '../utils/authValidate'

const router = useRouter()
const userStore = useUserStore()
const mallStore = useMallStore()
const loading = ref(false)
const captchaRef = ref(null)
const captchaCode = ref('')
// 邮箱验证码相关
const emailCode = ref('')
const sending = ref(false)
const countdown = ref(0)
const form = reactive({
  loginType: 'username',
  username: '',
  password: '',
  phone: '',
  email: ''
})

// 发送邮箱验证码 + 60 秒倒计时
async function sendCode() {
  if (!isValidEmail(form.email)) {
    return ElMessage.warning('请输入正确的邮箱地址')
  }
  sending.value = true
  try {
    await userStore.sendEmailCode(form.email)
    ElMessage.success('验证码已发送，请查收邮箱')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch {
    // request 已统一提示错误
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (form.loginType === 'emailCode') {
    // 邮箱验证码登录：免图形码、免密码
    if (!isValidEmail(form.email)) return ElMessage.warning('请输入正确的邮箱地址')
    if (!emailCode.value?.trim()) return ElMessage.warning('请输入邮箱验证码')
    loading.value = true
    try {
      await userStore.loginByCode(form.email.trim(), emailCode.value.trim())
      ElMessage.success('登录成功')
      router.push('/')
    } finally {
      loading.value = false
    }
    return
  }
  if (!captchaCode.value?.trim()) {
    return ElMessage.warning('请输入验证码')
  }
  // 按 loginType 校验不同字段：username / phone / email
  if (form.loginType === 'username') {
    if (!form.username.trim()) return ElMessage.warning('请输入用户名')
  } else if (form.loginType === 'phone') {
    if (!isValidPhone(form.phone)) return ElMessage.warning('请输入正确的11位手机号')
  } else if (!isValidEmail(form.email)) {
    return ElMessage.warning('请输入正确的邮箱地址')
  }
  if (!form.password) {
    return ElMessage.warning('请输入密码')
  }
  loading.value = true
  try {
    await userStore.login(trimAuthFields({
      ...form,
      captchaKey: captchaRef.value?.captchaKey,
      captchaCode: captchaCode.value
    }))
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    captchaRef.value?.refresh()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-button link class="auth-back" @click="router.push('/')">
      <el-icon><ArrowLeft /></el-icon> 返回首页
    </el-button>
    <div class="auth-card">
      <div class="auth-logo">
        <el-icon :size="36" color="#6366f1"><ShoppingBag /></el-icon>
      </div>
      <h2>欢迎回来</h2>
      <p class="subtitle">登录您的 {{ mallStore.siteName }} 账号</p>
      <el-form label-position="top" size="large">
        <el-form-item label="登录方式">
          <el-radio-group v-model="form.loginType" class="type-group">
            <el-radio-button value="username">账号</el-radio-button>
            <el-radio-button value="phone">手机</el-radio-button>
            <el-radio-button value="email">邮箱</el-radio-button>
            <el-radio-button value="emailCode">验证码</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.loginType === 'username'">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" />
          </el-form-item>
        </template>
        <template v-else-if="form.loginType === 'phone'">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" prefix-icon="Iphone" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" />
          </el-form-item>
        </template>
        <template v-else-if="form.loginType === 'email'">
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="请输入邮箱" prefix-icon="Message" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="请输入已注册邮箱" prefix-icon="Message" />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="code-row">
              <el-input v-model="emailCode" placeholder="请输入邮箱验证码" prefix-icon="Key" maxlength="6" />
              <el-button type="primary" :disabled="countdown > 0 || sending" :loading="sending" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </template>
        <CaptchaField v-if="form.loginType !== 'emailCode'" ref="captchaRef" v-model="captchaCode" />
        <el-button type="primary" :loading="loading" round class="submit-btn" @click="submit">登 录</el-button>
        <div class="auth-tip">还没有账号？<router-link to="/register">立即注册</router-link></div>
      </el-form>
    </div>
    <router-link to="/admin/login" class="admin-entry">
      <el-icon><Setting /></el-icon> 管理员登录
    </router-link>
  </div>
</template>

<style scoped>
.auth-logo {
  text-align: center;
  margin-bottom: 12px;
}
.auth-back {
  position: absolute;
  top: 24px;
  left: 24px;
  color: var(--mall-text-secondary);
  font-size: 14px;
}
.auth-page {
  position: relative;
}
.type-group {
  width: 100%;
  display: flex;
}
.type-group :deep(.el-radio-button) {
  flex: 1;
}
.type-group :deep(.el-radio-button__inner) {
  width: 100%;
}
.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-row .el-button {
  flex-shrink: 0;
}
.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
}
.admin-entry {
  position: fixed;
  right: 24px;
  bottom: 24px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid var(--mall-border);
  border-radius: 999px;
  box-shadow: var(--mall-shadow);
  transition: color 0.2s, border-color 0.2s;
}
.admin-entry:hover {
  color: var(--mall-primary);
  border-color: var(--mall-primary);
}
</style>
