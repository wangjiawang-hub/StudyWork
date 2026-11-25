<script setup>
import { useMallStore } from '../stores/mall'

const mallStore = useMallStore()
</script>

<template>
  <footer class="footer">
    <div class="inner">
      <div class="brand">
        <span class="logo">{{ mallStore.siteName }}</span>
        <p>品质好物 · 智能推荐 · 安全支付 · {{ mallStore.freeShippingTip }}</p>
      </div>
      <div class="links">
        <router-link to="/products">全部商品</router-link>
        <router-link to="/coupon">优惠券</router-link>
        <router-link to="/ai">智能客服</router-link>
      </div>
      <p class="copy">© 2026 {{ mallStore.siteName }} · 毕业设计演示系统</p>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: 48px;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  color: #94a3b8;
  padding: 40px 20px 32px;
}
.inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}
.brand .logo {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.02em;
}
.brand p {
  margin-top: 8px;
  font-size: 13px;
}
.links {
  display: flex;
  justify-content: center;
  gap: 28px;
  margin: 24px 0;
  font-size: 14px;
}
.links a {
  color: #cbd5e1;
  transition: color 0.2s;
}
.links a:hover {
  color: #fff;
}
.copy {
  font-size: 12px;
  opacity: 0.7;
}
</style>
