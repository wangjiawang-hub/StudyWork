<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { useMallStore } from '../../stores/mall'

const router = useRouter()
const userStore = useUserStore()
const mallStore = useMallStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

onMounted(() => {
  mallStore.load()
  // 进入后台登录页时清掉普通用户 token，避免携带无效凭证
  if (userStore.user?.role !== 1) {
    userStore.logout()
  }
})

async function submit() {
  if (!form.username.trim() || !form.password) {
    return ElMessage.warning('请输入管理员账号和密码')
  }
  loading.value = true
  try {
    await userStore.adminLogin({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    router.replace('/admin/users')
  } catch {
    // 错误已在拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-login">
    <div class="panel">
      <div class="brand">
        <el-icon :size="32"><Setting /></el-icon>
        <h1>{{ mallStore.siteName }} 后台管理</h1>
        <p>仅管理员账号可登录</p>
      </div>
      <el-form label-position="top" size="large" @submit.prevent="submit">
        <el-form-item label="管理员账号">
          <el-input v-model="form.username" placeholder="默认 admin" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submit">进入后台</el-button>
      </el-form>
      <router-link to="/" class="back">返回商城首页</router-link>
    </div>
  </div>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
  padding: 24px;
}
.panel {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 32px;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.35);
}
.brand {
  text-align: center;
  margin-bottom: 28px;
  color: #6366f1;
}
.brand h1 {
  margin: 12px 0 6px;
  font-size: 22px;
  color: #0f172a;
}
.brand p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}
.submit {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  font-weight: 600;
}
.back {
  display: block;
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #64748b;
}
.back:hover {
  color: #6366f1;
}
</style>
