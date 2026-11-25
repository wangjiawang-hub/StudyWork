<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import request from '../utils/request'
import PageHeader from '../components/PageHeader.vue'

const userStore = useUserStore()
const form = ref({ phone: '', email: '', avatar: '' })
const pwd = ref({ oldPassword: '', newPassword: '' })

onMounted(async () => {
  await userStore.fetchProfile()
  form.value = { phone: userStore.user.phone, email: userStore.user.email, avatar: userStore.user.avatar }
})

async function saveProfile() {
  await request.put('/auth/profile', form.value)
  ElMessage.success('保存成功')
}

async function savePwd() {
  await request.put('/auth/password', pwd.value)
  ElMessage.success('密码已修改')
}
</script>

<template>
  <div class="page-card">
    <PageHeader title="个人中心" icon="User" fallback="/" />
    <el-tabs>
      <el-tab-pane label="基本信息">
        <el-form label-width="80px" style="max-width:400px">
          <el-form-item label="用户名">{{ userStore.user?.username }}</el-form-item>
          <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-button type="primary" @click="saveProfile">保存</el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="修改密码">
        <el-form label-width="80px" style="max-width:400px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" /></el-form-item>
          <el-button type="primary" @click="savePwd">修改</el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="快捷入口">
        <el-button @click="$router.push({ path: '/address', query: { from: '/profile' } })">地址管理</el-button>
        <el-button @click="$router.push('/favorite')">我的收藏</el-button>
        <el-button @click="$router.push('/reviews')">我的评价</el-button>
        <el-button @click="$router.push('/orders')">我的订单</el-button>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
</style>
