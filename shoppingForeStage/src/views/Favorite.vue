<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import ProductCard from '../components/ProductCard.vue'
import PageHeader from '../components/PageHeader.vue'

const list = ref([])
onMounted(async () => {
  const res = await request.get('/user/favorite/list')
  list.value = res.data || []
})
</script>

<template>
  <div class="page-card">
    <PageHeader title="我的收藏" icon="Star" fallback="/profile" />
    <div v-if="list.length" class="product-grid">
      <router-link v-for="f in list" :key="f.id" :to="`/product/${f.productId}`" class="fav-link">
        <ProductCard :product="{ id: f.productId, name: f.productName, price: f.price, image: f.productImage }" />
      </router-link>
    </div>
    <el-empty v-else description="暂无收藏">
      <el-button type="primary" round @click="$router.push('/products')">去逛逛</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.fav-link { display: block; color: inherit; }
</style>
