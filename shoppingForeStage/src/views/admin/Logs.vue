<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'

const logs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function load() {
  const res = await request.get('/admin/logs', { params: { page: page.value, size: size.value } })
  logs.value = res.data.records || []
  total.value = res.data.total || 0
}

onMounted(load)
</script>

<template>
  <el-card>
    <h2>操作日志</h2>
    <p class="tip">记录后台管理员的关键操作，便于审计与排查。</p>
    <el-table :data="logs" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="操作人ID" width="100" />
      <el-table-column prop="operation" label="操作" min-width="120" />
      <el-table-column prop="method" label="方法" width="80" />
      <el-table-column prop="params" label="参数" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="120" />
      <el-table-column prop="createTime" label="时间" width="170" />
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
.tip {
  margin: -8px 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
