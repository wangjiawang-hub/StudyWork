<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref(null)
const shippingId = ref(null)
const statusMap = ['待付款', '待发货', '待收货', '已完成', '已取消', '售后中']

async function load() {
  const res = await request.get('/admin/orders', {
    params: {
      page: page.value,
      size: size.value,
      status: statusFilter.value ?? undefined
    }
  })
  orders.value = res.data.records || []
  total.value = res.data.total || 0
}

async function updateStatus(row) {
  await request.put('/admin/orders/status', null, { params: { id: row.id, status: row.orderStatus } })
  ElMessage.success('订单状态已更新')
  load()
}

async function ship(id) {
  try {
    await ElMessageBox.confirm('将为该订单生成模拟物流单号并标记为已发货，是否继续？', '模拟发货', { type: 'info' })
    shippingId.value = id
    const res = await request.post('/admin/orders/ship', null, { params: { id } })
    ElMessage.success(`模拟发货成功：${res.data.logisticsCompany} ${res.data.logisticsNo}`)
    load()
  } catch {
    // 用户取消或失败
  } finally {
    shippingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <h2>订单管理</h2>
      <el-select v-model="statusFilter" clearable placeholder="按状态筛选" style="width:140px" @change="page=1;load()">
        <el-option v-for="(label, idx) in statusMap" :key="idx" :label="label" :value="idx" />
      </el-select>
    </div>

    <el-table :data="orders" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="payAmount" label="实付" width="100">
        <template #default="{ row }">¥{{ row.payAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-select v-model="row.orderStatus" size="small" @change="updateStatus(row)">
            <el-option v-for="(label, idx) in statusMap" :key="idx" :label="label" :value="idx" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="物流" min-width="160">
        <template #default="{ row }">
          <span v-if="row.logisticsNo">{{ row.logisticsCompany }} {{ row.logisticsNo }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.orderStatus === 1"
            type="primary"
            size="small"
            :loading="shippingId === row.id"
            @click="ship(row.id)"
          >
            模拟发货
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </el-card>
</template>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
.muted { color: var(--el-text-color-secondary); }
</style>
