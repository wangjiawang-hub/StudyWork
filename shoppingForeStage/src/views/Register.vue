<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useMallStore } from '../stores/mall'
import CaptchaField from '../components/CaptchaField.vue'
import { isValidPhone, trimAuthFields } from '../utils/authValidate'

const router = useRouter()
const userStore = useUserStore()
const mallStore = useMallStore()
const captchaRef = ref(null)
const captchaCode = ref('')
const loading = ref(false)
const form = reactive({ username: '', password: '', phone: '', email: '' })

async function submit() {
  if (!form.username.trim()) {
    return ElMessage.warning('请输入用户名')
  }
  if (!form.password || form.password.length < 6) {
    return ElMessage.warning('密码至少6位')
  }
  if (form.phone.trim() && !isValidPhone(form.phone)) {
    return ElMessage.warning('请输入正确的11位手机号')
  }
  if (!captchaCode.value?.trim()) {
    return ElMessage.warning('请输入验证码')
  }
  loading.value = true
  try {
    await userStore.register(trimAuthFields({
      ...form,
      captchaKey: captchaRef.value?.captchaKey,
      captchaCode: captchaCode.value
    }))
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
      <h2>创建账号</h2>
      <p class="subtitle">加入 {{ mallStore.siteName }}，开启品质购物之旅</p>
      <el-form label-position="top" size="large">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="手机号（选填）">
          <el-input v-model="form.phone" placeholder="11位手机号，如 13800138000" maxlength="11" prefix-icon="Iphone" />
        </el-form-item>
        <CaptchaField ref="captchaRef" v-model="captchaCode" />
        <el-button type="primary" :loading="loading" round class="submit-btn" @click="submit">注 册</el-button>
        <div class="auth-tip">已有账号？<router-link to="/login">立即登录</router-link></div>
      </el-form>
    </div>
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
.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
}
</style>
