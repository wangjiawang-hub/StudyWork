<script setup>
/**
 * 图形验证码：挂载时拉取 /auth/captcha，父组件通过 ref 取 captchaKey 随登录/注册提交。
 * 验证失败时父组件调用 refresh() 换一张。
 */
import { ref, onMounted } from 'vue'
import request from '../utils/request'

const model = defineModel({ type: String, default: '' })

const captchaKey = ref('')
const displayCode = ref('')
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    captchaKey.value = Date.now().toString()
    const res = await request.get('/auth/captcha', { params: { key: captchaKey.value } })
    displayCode.value = res.data?.code || ''
    model.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(refresh)

defineExpose({ captchaKey, refresh })
</script>

<template>
  <el-form-item label="验证码">
    <div class="captcha-row">
      <span class="captcha-display" title="页面验证码">{{ displayCode || '----' }}</span>
      <el-input
        v-model="model"
        placeholder="请输入上方四位验证码"
        maxlength="4"
        inputmode="numeric"
      />
      <el-button :loading="loading" @click="refresh">换一张</el-button>
    </div>
  </el-form-item>
</template>

<style scoped>
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: center;
}
.captcha-display {
  flex-shrink: 0;
  min-width: 72px;
  padding: 0 12px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: #4338ca;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  border: 1px dashed #a5b4fc;
  border-radius: 8px;
  user-select: none;
}
.captcha-row .el-input {
  flex: 1;
}
</style>
