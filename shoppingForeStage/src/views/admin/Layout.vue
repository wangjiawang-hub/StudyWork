<script setup>
import { computed } from 'vue'
import { useRouter, useRoute, RouterView } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const username = computed(() => userStore.user?.username || '管理员')

function logout() {
  userStore.logout()
  router.replace('/admin/login')
}
</script>

<template>
  <div class="admin">
    <aside class="sidebar">
      <div class="sidebar-head">
        <el-icon :size="22"><Setting /></el-icon>
        <span>后台管理</span>
      </div>
      <nav>
        <router-link to="/admin/dashboard"><el-icon><TrendCharts /></el-icon> 数据看板</router-link>
        <router-link to="/admin/users"><el-icon><User /></el-icon> 用户管理</router-link>
        <router-link to="/admin/categories"><el-icon><Menu /></el-icon> 商品分类</router-link>
        <router-link to="/admin/products"><el-icon><Goods /></el-icon> 商品管理</router-link>
        <router-link to="/admin/coupons"><el-icon><Ticket /></el-icon> 优惠券管理</router-link>
        <router-link to="/admin/orders"><el-icon><List /></el-icon> 订单管理</router-link>
        <router-link to="/admin/config"><el-icon><Tools /></el-icon> 系统配置</router-link>
        <router-link to="/admin/logs"><el-icon><Document /></el-icon> 操作日志</router-link>
        <router-link to="/admin/chat"><el-icon><Service /></el-icon> 客服会话</router-link>
        <router-link to="/admin/knowledge"><el-icon><Collection /></el-icon> AI 知识库</router-link>
      </nav>
      <div class="sidebar-foot">
        <div class="user-info">
          <el-icon><Avatar /></el-icon>
          <span>{{ username }}</span>
        </div>
        <router-link to="/" class="back-link">
          <el-icon><Back /></el-icon> 返回商城
        </router-link>
        <el-button link type="danger" class="logout" @click="logout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </el-button>
      </div>
    </aside>
    <main class="admin-main">
      <RouterView :key="route.fullPath" />
    </main>
  </div>
</template>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
  background: var(--mall-bg);
}
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  color: #cbd5e1;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  flex-shrink: 0;
}
.sidebar-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  padding: 0 12px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 16px;
}
nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}
nav a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}
nav a:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}
nav a.router-link-active {
  color: #fff;
  background: var(--mall-primary);
}
.sidebar-foot {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 16px;
  margin-top: 16px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px 12px;
  color: #e2e8f0;
  font-size: 13px;
}
.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: #64748b;
  font-size: 13px;
}
.back-link:hover {
  color: #fff;
}
.logout {
  width: 100%;
  justify-content: flex-start;
  padding: 8px 12px;
  color: #f87171 !important;
}
.admin-main {
  flex: 1;
  padding: 28px;
  overflow: auto;
}
.admin-main :deep(.el-card) {
  border-radius: var(--mall-radius);
  border: 1px solid var(--mall-border);
  box-shadow: var(--mall-shadow);
}
.admin-main :deep(h2) {
  margin: 0 0 16px;
  font-size: 20px;
}
.admin-main :deep(.toolbar) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.admin-main :deep(.filters) {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
