<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import ProductCard from '../components/ProductCard.vue'

const router = useRouter()
const keyword = ref('')
const hotProducts = ref([])
const recommendProducts = ref([])

const banners = [
  { title: '品质好物 · 一站购齐', sub: '精选商品，智能推荐，畅享购物', gradient: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)' },
  { title: '限时优惠 · 领券立减', sub: '新人专享券，满减活动进行中', gradient: 'linear-gradient(135deg, #f97316 0%, #fb923c 100%)' },
  { title: '极速配送 · 售后无忧', sub: '物流实时追踪，7天无理由退换', gradient: 'linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%)' }
]

const features = [
  { icon: 'Van', title: '极速配送', desc: '全国主要城市次日达' },
  { icon: 'CircleCheck', title: '正品保障', desc: '官方渠道，品质承诺' },
  { icon: 'Refresh', title: '7天退换', desc: '无忧售后，放心购买' },
  { icon: 'Service', title: 'AI 客服', desc: '24小时智能在线咨询' }
]

onMounted(async () => {
  // 首页数据：并行拉热销与推荐（无独立 Home 后端，由 goods 服务提供）
  try {
    const [hot, rec] = await Promise.all([
      request.get('/goods/hot', { params: { limit: 8 } }),
      request.get('/goods/recommend', { params: { limit: 8 } })
    ])
    hotProducts.value = hot.data || []
    recommendProducts.value = rec.data || []
  } catch {
    hotProducts.value = []
    recommendProducts.value = []
  }
})

const goDetail = id => router.push(`/product/${id}`)

function search() {
  const q = keyword.value.trim()
  // 搜索不在首页调 API，带 keyword 跳转商品列表页
  router.push({ path: '/products', query: q ? { keyword: q } : {} })
}
</script>

<template>
  <div class="home">
    <el-carousel height="320px" class="banner" :interval="5000">
      <el-carousel-item v-for="(b, i) in banners" :key="i">
        <div class="banner-item" :style="{ background: b.gradient }">
          <div class="banner-content">
            <h2>{{ b.title }}</h2>
            <p>{{ b.sub }}</p>
            <el-button round size="large" @click="router.push('/products')">立即选购</el-button>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <div class="search-bar page-card">
      <el-input
        v-model="keyword"
        size="large"
        placeholder="搜索商品名称、关键词..."
        clearable
        @keyup.enter="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" size="large" round @click="search">搜索</el-button>
    </div>

    <div class="features">
      <div v-for="f in features" :key="f.title" class="feature-item">
        <div class="feature-icon">
          <el-icon :size="22"><component :is="f.icon" /></el-icon>
        </div>
        <div>
          <strong>{{ f.title }}</strong>
          <span>{{ f.desc }}</span>
        </div>
      </div>
    </div>

    <section class="section page-card">
      <div class="section-head">
        <h3><el-icon><TrendCharts /></el-icon> 热门商品</h3>
        <span class="more" @click="router.push('/products')">查看全部 →</span>
      </div>
      <div class="product-grid">
        <ProductCard v-for="p in hotProducts" :key="p.id" :product="p" @click="goDetail(p.id)" />
      </div>
      <el-empty v-if="!hotProducts.length" description="暂无热门商品" />
    </section>

    <section class="section page-card">
      <div class="section-head">
        <h3><el-icon><Star /></el-icon> 为你推荐</h3>
        <span class="more" @click="router.push('/products')">查看全部 →</span>
      </div>
      <div class="product-grid">
        <ProductCard v-for="p in recommendProducts" :key="p.id" :product="p" @click="goDetail(p.id)" />
      </div>
      <el-empty v-if="!recommendProducts.length" description="暂无推荐商品" />
    </section>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 28px;
}
.banner {
  border-radius: var(--mall-radius);
  overflow: hidden;
  box-shadow: var(--mall-shadow);
}
.search-bar {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
}
.search-bar .el-input {
  flex: 1;
}
.banner-item {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-content {
  text-align: center;
  color: #fff;
}
.banner-content h2 {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.03em;
  margin-bottom: 12px;
}
.banner-content p {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 24px;
}
.banner-content .el-button {
  background: rgba(255, 255, 255, 0.95);
  color: var(--mall-primary);
  border: none;
  font-weight: 600;
}
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--mall-surface);
  padding: 18px 20px;
  border-radius: var(--mall-radius);
  border: 1px solid var(--mall-border);
}
.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--el-color-primary-light-9);
  color: var(--mall-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.feature-item strong {
  display: block;
  font-size: 14px;
  margin-bottom: 2px;
}
.feature-item span {
  font-size: 12px;
  color: var(--mall-text-secondary);
}
.section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
}
@media (max-width: 768px) {
  .features { grid-template-columns: repeat(2, 1fr); }
  .banner-content h2 { font-size: 24px; }
}
</style>
