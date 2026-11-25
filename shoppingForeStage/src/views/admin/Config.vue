<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useMallStore } from '../../stores/mall'

const configs = ref([])
const mallStore = useMallStore()

async function load() {
  const res = await request.get('/admin/config')
  configs.value = res.data || []
}

async function save(row) {
  await request.put('/admin/config', null, {
    params: { key: row.configKey, value: row.configValue }
  })
  await mallStore.load()
  ElMessage.success('配置已更新')
}

onMounted(load)
</script>

<template>
  <el-card>
    <h2>系统配置</h2>
    <p class="tip">维护商城基础参数；站点名称与包邮规则会同步到前台展示与订单结算。</p>
    <el-table :data="configs" stripe>
      <el-table-column prop="configKey" label="配置键" width="200" />
      <el-table-column label="配置值" min-width="240">
        <template #default="{ row }">
          <el-input v-model="row.configValue" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="说明" min-width="160" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="save(row)">保存</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.tip {
  margin: -8px 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
