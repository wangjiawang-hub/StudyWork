<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { productImage } from '../utils/productImage'
import PageHeader from '../components/PageHeader.vue'

const list = ref([])
const loading = ref(true)

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  try {
    const res = await request.get('/user/review/mine')
    list.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-card">
    <PageHeader title="我的评价" icon="ChatLineSquare" fallback="/profile" />
    <div v-loading="loading">
      <div v-if="list.length" class="review-list">
        <div v-for="item in list" :key="item.id" class="review-item">
          <router-link :to="`/product/${item.productId}`" class="product-link">
            <img
              :src="productImage(item.productImage, item.productId, 120)"
              :alt="item.productName"
              class="product-thumb"
            />
            <div class="product-info">
              <span class="product-name">{{ item.productName || '商品已下架' }}</span>
              <el-rate :model-value="item.rating" disabled size="small" />
            </div>
          </router-link>
          <p class="review-content">{{ item.content }}</p>
          <p v-if="item.reply" class="review-reply">商家回复：{{ item.reply }}</p>
          <p class="review-time">{{ formatTime(item.createTime) }}</p>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="暂无评价">
        <el-button type="primary" round @click="$router.push('/orders')">去订单里评价</el-button>
      </el-empty>
    </div>
  </div>
</template>

<style scoped>
.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.review-item {
  padding: 16px;
  border: 1px solid var(--mall-border);
  border-radius: var(--mall-radius-sm);
  background: #fff;
}
.product-link {
  display: flex;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
}
.product-link:hover .product-name {
  color: var(--el-color-primary);
}
.product-thumb {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: var(--mall-bg);
}
.product-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.product-name {
  font-weight: 600;
  line-height: 1.4;
}
.review-content {
  margin: 12px 0 8px;
  color: var(--mall-text-secondary);
  line-height: 1.7;
}
.review-reply {
  margin: 0 0 8px;
  padding: 10px 12px;
  background: var(--mall-bg);
  border-radius: 8px;
  font-size: 14px;
  color: var(--mall-text-secondary);
  line-height: 1.6;
}
.review-time {
  margin: 0;
  font-size: 13px;
  color: #94a3b8;
}
</style>
