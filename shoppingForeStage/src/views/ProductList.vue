<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../utils/request'
import ProductCard from '../components/ProductCard.vue'

const router = useRouter()
const route = useRoute()
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const products = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await request.get('/goods/search', {
      params: { keyword: keyword.value, page: page.value, size: 12 }
    })
    products.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.keyword) {
    keyword.value = String(route.query.keyword)
  }
  load()
})

watch(() => route.query.keyword, val => {
  keyword.value = val ? String(val) : ''
  page.value = 1
  load()
})
const goDetail = id => router.push(`/product/${id}`)
</script>

<template>
  <div class="products-page">
    <div class="search-bar page-card">
      <el-input
        v-model="keyword"
        placeholder="搜索您想要的商品..."
        size="large"
        clearable
        @keyup.enter="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" size="large" :loading="loading" @click="load">搜索</el-button>
    </div>

    <div v-loading="loading" class="page-card list-section">
      <h2 class="page-title">全部商品</h2>
      <div v-if="products.length" class="product-grid">
        <ProductCard v-for="p in products" :key="p.id" :product="p" @click="goDetail(p.id)" />
      </div>
      <el-empty v-else description="暂无商品" />
      <div v-if="total > 12" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="12"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.products-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.search-bar {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
}
.search-bar .el-input {
  flex: 1;
}
.list-section {
  min-height: 400px;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
