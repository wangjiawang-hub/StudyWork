<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import PageHeader from '../components/PageHeader.vue'

const available = ref([])
const mine = ref([])

async function load() {
  const [a, m] = await Promise.all([request.get('/coupon/list'), request.get('/coupon/my')])
  available.value = a.data || []
  mine.value = m.data || []
}

async function receive(id) {
  await request.post('/coupon/receive', null, { params: { couponId: id } })
  ElMessage.success('领取成功')
  load()
}

onMounted(load)
</script>

<template>
  <div class="coupon-page">
    <div class="page-card">
      <PageHeader title="可领取优惠券" icon="Ticket" fallback="/" />
      <el-table :data="available" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="threshold" label="门槛">
          <template #default="{ row }">满 ¥{{ row.threshold }}</template>
        </el-table-column>
        <el-table-column prop="reduceAmount" label="减免">
          <template #default="{ row }"><span class="reduce">-¥{{ row.reduceAmount }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" round @click="receive(row.id)">领取</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!available.length" description="暂无可领优惠券" />
    </div>
    <div class="page-card">
      <PageHeader title="我的优惠券" icon="Collection" fallback="/" />
      <el-table :data="mine" stripe>
        <el-table-column prop="coupon.name" label="名称" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">
              {{ row.status === 0 ? '未使用' : '已使用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!mine.length" description="暂无优惠券" />
    </div>
  </div>
</template>

<style scoped>
.coupon-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.reduce {
  color: #ef4444;
  font-weight: 600;
}
</style>
