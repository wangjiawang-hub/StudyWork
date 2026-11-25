<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useCartStore } from '../stores/cart'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const cartStore = useCartStore()
const list = ref([])
const selected = ref([])

onMounted(load)
async function load() {
  const res = await request.get('/cart/list')
  list.value = res.data || []
}

async function updateQty(row) {
  await request.post('/cart/update', { id: row.id, quantity: row.quantity })
  cartStore.refreshCount()
}

async function remove(row) {
  await request.delete('/cart/delete', { params: { id: row.id } })
  ElMessage.success('已删除')
  load()
  cartStore.refreshCount()
}

function checkout() {
  if (!selected.value.length) return ElMessage.warning('请选择商品')
  // 跨页传递选中项：Checkout 按 ID 过滤 /cart/list
  localStorage.setItem('checkoutCartIds', JSON.stringify(selected.value))
  router.push('/checkout')
}
</script>

<template>
  <div class="page-card cart-page">
    <PageHeader title="我的购物车" icon="ShoppingCart" fallback="/products" back-text="继续购物">
      <template #actions>
        <span class="count">共 {{ list.length }} 件商品</span>
      </template>
    </PageHeader>

    <el-table
      v-if="list.length"
      :data="list"
      stripe
      @selection-change="rows => selected = rows.map(r => r.id)"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column label="商品" min-width="200">
        <template #default="{ row }">
          {{ row.productName }}
          <span v-if="row.skuSpec" class="sku-spec">{{ row.skuSpec }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单价" width="120">
        <template #default="{ row }">
          <span class="cell-price">¥{{ row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="180">
        <template #default="{ row }">
          <el-input-number v-model="row.quantity" :min="1" size="small" @change="updateQty(row)" />
        </template>
      </el-table-column>
      <el-table-column label="小计" width="120">
        <template #default="{ row }">
          <span class="cell-price">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" link @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else description="购物车是空的，去逛逛吧">
      <el-button type="primary" round @click="router.push('/products')">去购物</el-button>
    </el-empty>

    <div v-if="list.length" class="footer">
      <el-button round @click="router.push('/products')">继续购物</el-button>
      <el-button type="primary" size="large" round @click="checkout">
        去结算 ({{ selected.length }} 件)
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  min-height: 400px;
}
.count {
  font-size: 14px;
  color: var(--mall-text-secondary);
}
.cell-price {
  color: #ef4444;
  font-weight: 600;
}
.sku-spec {
  display: inline-block;
  margin-left: 8px;
  font-size: 12px;
  color: var(--mall-text-secondary);
  background: var(--mall-bg);
  padding: 2px 8px;
  border-radius: 4px;
}
.footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--mall-border);
  text-align: right;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
