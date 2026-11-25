<script setup>
import { onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { useMallStore } from '../stores/mall'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()
const mallStore = useMallStore()

function refreshCartIfLogin() {
  if (userStore.token) cartStore.refreshCount()
}

onMounted(refreshCartIfLogin)

watch(() => route.path, refreshCartIfLogin)

const navItems = [
  { path: '/', label: '首页', icon: 'House' },
  { path: '/products', label: '商品', icon: 'Goods' },
  { path: '/cart', label: '购物车', icon: 'ShoppingCart', badge: true },
  { path: '/orders', label: '订单', icon: 'List' },
  { path: '/coupon', label: '优惠券', icon: 'Ticket' },
  { path: '/ai', label: 'AI咨询', icon: 'ChatDotRound' },
  { path: '/chat', label: '客服', icon: 'Service' }
]
</script>

<template>
  <header class="header">
    <div class="inner">
      <div class="logo" @click="router.push('/')">
        <el-icon :size="26"><ShoppingBag /></el-icon>
        <span>{{ mallStore.siteName }}</span>
      </div>
      <nav>
        <a
          v-for="item in navItems"
          :key="item.path"
          class="nav-link"
          @click="router.push(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
          <span v-if="item.badge && cartStore.count" class="badge">{{ cartStore.count }}</span>
        </a>
      </nav>
      <div class="user-area">
        <template v-if="userStore.token">
          <el-dropdown trigger="click">
            <span class="user-btn">
              <el-icon><User /></el-icon>
              {{ userStore.user?.username }}
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push({ path: '/address', query: { from: route.path } })">收货地址</el-dropdown-item>
                <el-dropdown-item @click="router.push('/favorite')">我的收藏</el-dropdown-item>
                <el-dropdown-item @click="router.push('/reviews')">我的评价</el-dropdown-item>
                <el-dropdown-item v-if="userStore.user?.role === 1" divided @click="router.push('/admin/users')">
                  后台管理
                </el-dropdown-item>
                <el-dropdown-item divided @click="userStore.logout(); router.push('/login')">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button v-else type="primary" round @click="router.push('/login')">登录 / 注册</el-button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--mall-border);
  position: sticky;
  top: 0;
  z-index: 100;
}
.inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  cursor: pointer;
  color: var(--mall-primary);
  letter-spacing: -0.03em;
  flex-shrink: 0;
}
nav {
  display: flex;
  gap: 4px;
  flex: 1;
}
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px;
  border-radius: 20px;
  cursor: pointer;
  color: var(--mall-text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  position: relative;
}
.nav-link:hover {
  color: var(--mall-primary);
  background: var(--el-color-primary-light-9);
}
.badge {
  position: absolute;
  top: 2px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--mall-accent);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.user-area {
  flex-shrink: 0;
}
.user-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--mall-text);
  transition: background 0.2s;
}
.user-btn:hover {
  background: var(--el-color-primary-light-9);
}
.arrow {
  font-size: 12px;
  color: var(--mall-text-secondary);
}
@media (max-width: 900px) {
  nav { display: none; }
}
</style>
