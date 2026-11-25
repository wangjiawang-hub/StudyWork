<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { productImage } from '../utils/productImage'
import { useCartStore } from '../stores/cart'
import { useUserStore } from '../stores/user'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()
const product = ref(null)
const reviews = ref([])
const isFavorite = ref(false)
const qty = ref(1)

// ---- SKU 选择 ----
const skuList = ref([])
const selectedSpecs = ref({}) // { 规格名: 规格值 }
const selectedSku = computed(() => {
  if (!skuList.value.length) return null
  return skuList.value.find(sku => {
    return (sku.specs || []).every(sp => selectedSpecs.value[sp.specName] === sp.specValue)
  })
})
const currentPrice = computed(() => {
  if (selectedSku.value) return selectedSku.value.price
  return product.value?.price
})
const currentStock = computed(() => {
  if (selectedSku.value) return selectedSku.value.stock
  return product.value?.stock
})
// 规格维度列表：从 skuList 的 specs 中提取去重
const specDimensions = computed(() => {
  const map = new Map()
  for (const sku of skuList.value) {
    for (const spec of (sku.specs || [])) {
      if (!map.has(spec.specName)) map.set(spec.specName, [])
      const values = map.get(spec.specName)
      if (!values.includes(spec.specValue)) values.push(spec.specValue)
    }
  }
  return Array.from(map.entries()).map(([name, values]) => ({ name, values }))
})
const skuSpecText = computed(() => {
  if (!selectedSku.value) return null
  return (selectedSku.value.specs || []).map(s => s.specValue).join('/')
})

function scrollToTop() {
  window.scrollTo(0, 0)
}

async function loadProduct(id) {
  scrollToTop()
  try {
    const res = await request.get('/goods/detail', { params: { id } })
    product.value = res.data
    // 加载 SKU
    const skuRes = await request.get('/goods/skus', { params: { productId: id } })
    skuList.value = skuRes.data || []
    // 默认选中第一个可选规格值
    selectedSpecs.value = {}
    for (const dim of specDimensions.value) {
      if (dim.values.length) selectedSpecs.value[dim.name] = dim.values[0]
    }
    const rev = await request.get('/user/review/list', { params: { productId: id } })
    reviews.value = rev.data || []
    if (userStore.token) {
      const fav = await request.get('/user/favorite/check', { params: { productId: id } })
      isFavorite.value = fav.data.favorite
    } else {
      isFavorite.value = false
    }
    qty.value = 1
  } catch {
    ElMessage.error('商品加载失败')
  }
}

onMounted(() => loadProduct(route.params.id))

watch(() => route.params.id, (id) => {
  if (id) loadProduct(id)
})

async function addCart() {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再加入购物车')
    return router.push('/login')
  }
  // 如果有 SKU 但未选完整，提示
  if (skuList.value.length && !selectedSku.value) {
    return ElMessage.warning('请选择完整的商品规格')
  }
  try {
    await request.post('/cart/add', {
      productId: product.value.id,
      quantity: qty.value,
      skuId: selectedSku.value?.id || null,
      skuSpec: skuSpecText.value
    })
    await cartStore.refreshCount()
    ElMessage.success('已加入购物车')
  } catch {
    // 错误已在拦截器提示
  }
}

async function toggleFavorite() {
  if (!userStore.token) return router.push('/login')
  try {
    if (isFavorite.value) {
      await request.delete('/user/favorite', { params: { productId: product.value.id } })
      isFavorite.value = false
    } else {
      await request.post('/user/favorite', null, { params: { productId: product.value.id } })
      isFavorite.value = true
    }
  } catch {
    // 错误已在拦截器提示
  }
}
</script>

<template>
  <div v-if="product" class="detail-page">
    <PageHeader :title="product.name" icon="Goods" fallback="/products" back-text="返回商品列表" />
    <div class="detail-main page-card">
      <div class="gallery">
        <img :src="productImage(product.image, product.id, 500)" :alt="product.name" />
      </div>
      <div class="info">
        <h1>{{ product.name }}</h1>
        <div class="price-row">
          <span class="price">¥{{ currentPrice }}</span>
          <span class="stock">库存 {{ currentStock }} 件</span>
        </div>
        <p class="desc">{{ product.description || '暂无商品描述' }}</p>
        <!-- SKU 规格选择 -->
        <div v-if="specDimensions.length" class="sku-section">
          <div v-for="dim in specDimensions" :key="dim.name" class="spec-group">
            <span class="spec-label">{{ dim.name }}</span>
            <div class="spec-options">
              <el-button
                v-for="val in dim.values"
                :key="val"
                size="small"
                :type="selectedSpecs[dim.name] === val ? 'primary' : 'default'"
                round
                @click="selectedSpecs[dim.name] = val"
              >{{ val }}</el-button>
            </div>
          </div>
        </div>
        <div class="actions">
          <span class="qty-label">数量</span>
          <el-input-number v-model="qty" :min="1" :max="currentStock" size="large" />
          <el-button type="primary" size="large" round @click="addCart">
            <el-icon><ShoppingCart /></el-icon> 加入购物车
          </el-button>
          <el-button
            size="large"
            round
            :type="isFavorite ? 'warning' : 'default'"
            @click="toggleFavorite"
          >
            <el-icon><Star /></el-icon>
            {{ isFavorite ? '已收藏' : '收藏' }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="reviews page-card">
      <h3 class="page-title">商品评价</h3>
      <div v-if="reviews.length" class="review-list">
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <div class="review-head">
            <strong>{{ r.username }}</strong>
            <el-rate :model-value="r.rating" disabled size="small" />
          </div>
          <p>{{ r.content }}</p>
        </div>
      </div>
      <el-empty v-else description="暂无评价" />
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.detail-main {
  display: flex;
  gap: 40px;
  padding: 32px;
}
.gallery {
  flex-shrink: 0;
  width: 420px;
  border-radius: var(--mall-radius);
  overflow: hidden;
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
}
.gallery img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
}
.info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.info h1 {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.02em;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin: 16px 0;
}
.price {
  color: #ef4444;
  font-size: 32px;
  font-weight: 700;
}
.stock {
  font-size: 14px;
  color: var(--mall-text-secondary);
}
.desc {
  color: var(--mall-text-secondary);
  line-height: 1.7;
  flex: 1;
}
.sku-section {
  margin: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.spec-group {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.spec-label {
  font-size: 14px;
  color: var(--mall-text-secondary);
  min-width: 50px;
  line-height: 32px;
}
.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--mall-border);
}
.qty-label {
  font-size: 14px;
  color: var(--mall-text-secondary);
}
.review-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--mall-border);
}
.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.review-item p {
  color: var(--mall-text-secondary);
  line-height: 1.6;
}
@media (max-width: 768px) {
  .detail-main { flex-direction: column; padding: 20px; }
  .gallery { width: 100%; }
}
</style>
