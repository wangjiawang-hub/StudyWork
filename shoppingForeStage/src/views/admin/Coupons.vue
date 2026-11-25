<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import request from '../../utils/request'

const coupons = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const dialogVisible = ref(false)

// 表单：type 0=满减 1=折扣
const emptyForm = {
  id: null, name: '', type: 0, threshold: 0, reduceAmount: null,
  discountRate: null, maxDiscount: null, totalQuantity: 100,
  perUserLimit: 1, range: [], status: 1
}
const form = ref({ ...emptyForm })

function fmtDate(ts) {
  return ts ? dayjs(ts).format('YYYY-MM-DD') : '-'
}

function ruleText(row) {
  if (row.type === 0) return `满 ${row.threshold} 减 ${row.reduceAmount}`
  return `${(row.discountRate * 10).toFixed(1)} 折${row.maxDiscount ? `（最高减 ${row.maxDiscount}）` : ''}`
}

async function load() {
  const res = await request.get('/admin/coupons', { params: { page: page.value, size: size.value, keyword: keyword.value || undefined } })
  coupons.value = res.data.records || []
  total.value = res.data.total || 0
}

function openAdd() {
  form.value = { ...emptyForm }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = {
    id: row.id, name: row.name, type: row.type, threshold: row.threshold,
    reduceAmount: row.reduceAmount, discountRate: row.discountRate, maxDiscount: row.maxDiscount,
    totalQuantity: row.totalQuantity, perUserLimit: row.perUserLimit, status: row.status,
    range: [fmtDate(row.expireStart), fmtDate(row.expireEnd)]
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name?.trim()) return ElMessage.warning('请输入优惠券名称')
  if (!form.value.range || form.value.range.length !== 2) return ElMessage.warning('请选择有效期')
  const payload = {
    id: form.value.id, name: form.value.name.trim(), type: form.value.type,
    threshold: form.value.threshold || 0, reduceAmount: form.value.type === 0 ? form.value.reduceAmount : null,
    discountRate: form.value.type === 1 ? form.value.discountRate : null, maxDiscount: form.value.type === 1 ? form.value.maxDiscount : null,
    totalQuantity: form.value.totalQuantity, perUserLimit: form.value.perUserLimit,
    expireStart: form.value.range[0], expireEnd: form.value.range[1], status: form.value.status
  }
  await request.post('/admin/coupons', payload)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row) {
  await request.put('/admin/coupons/status', null, { params: { id: row.id, status: row.status === 1 ? 0 : 1 } })
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  load()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除优惠券「${row.name}」？用户已领取的该券会一并删除。`, '提示', { type: 'warning' })
    await request.delete('/admin/coupons', { params: { id: row.id } })
    ElMessage.success('已删除')
    load()
  } catch {
    // 用户取消
  }
}

onMounted(load)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <h2>优惠券管理</h2>
      <div class="filters">
        <el-input v-model="keyword" placeholder="按名称搜索" clearable style="width: 200px" @keyup.enter="page = 1; load()" @clear="page = 1; load()" />
        <el-button @click="page = 1; load()">搜索</el-button>
        <el-button type="primary" @click="openAdd">新增优惠券</el-button>
      </div>
    </div>

    <el-table :data="coupons" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 0 ? 'primary' : 'success'" size="small">{{ row.type === 0 ? '满减' : '折扣' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="规则" min-width="150">
        <template #default="{ row }">{{ ruleText(row) }}</template>
      </el-table-column>
      <el-table-column label="已领/总量" width="100">
        <template #default="{ row }">{{ row.usedQuantity }} / {{ row.totalQuantity }}</template>
      </el-table-column>
      <el-table-column label="每人限领" width="90" prop="perUserLimit" />
      <el-table-column label="有效期" width="200">
        <template #default="{ row }">{{ fmtDate(row.expireStart) }} ~ {{ fmtDate(row.expireEnd) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="load"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑优惠券' : '新增优惠券'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：满100减10" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">满减</el-radio>
            <el-radio :value="1">折扣</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.threshold" :min="0" :precision="2" />
          <span class="tip">订单满该金额可用，0 表示无门槛</span>
        </el-form-item>
        <el-form-item v-if="form.type === 0" label="减免金额" required>
          <el-input-number v-model="form.reduceAmount" :min="0.01" :precision="2" />
        </el-form-item>
        <template v-if="form.type === 1">
          <el-form-item label="折扣率" required>
            <el-input-number v-model="form.discountRate" :min="0.01" :max="0.99" :step="0.05" :precision="2" />
            <span class="tip">0.85 表示 85 折</span>
          </el-form-item>
          <el-form-item label="最高优惠">
            <el-input-number v-model="form.maxDiscount" :min="0" :precision="2" />
            <span class="tip">留空表示不封顶</span>
          </el-form-item>
        </template>
        <el-form-item label="发放总量">
          <el-input-number v-model="form.totalQuantity" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" />
        </el-form-item>
        <el-form-item label="有效期" required>
          <el-date-picker v-model="form.range" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始日期" end-placeholder="结束日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上架状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.tip {
  margin-left: 10px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
