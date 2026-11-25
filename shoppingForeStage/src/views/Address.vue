<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import PageHeader from '../components/PageHeader.vue'
import { usePageBack } from '../composables/usePageBack'

const list = ref([])
const form = ref({ receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
const dialog = ref(false)
const saving = ref(false)
const { goBack, returnPath, backLabel } = usePageBack('/profile')

onMounted(load)

async function load() {
  const res = await request.get('/user/address/list')
  list.value = res.data || []
}

function openAdd() {
  form.value = { receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: list.value.length ? 0 : 1 }
  dialog.value = true
}

function openEdit(row) {
  form.value = { ...row, isDefault: row.isDefault || 0 }
  dialog.value = true
}

function closeDialog() {
  dialog.value = false
}

async function save() {
  if (!form.value.receiverName?.trim()) return ElMessage.warning('请填写收货人'), false
  if (!form.value.phone?.trim()) return ElMessage.warning('请填写联系电话'), false
  if (!form.value.detail?.trim()) return ElMessage.warning('请填写详细地址'), false
  saving.value = true
  try {
    // 有 id 为更新，无 id 为新增；后端处理首地址默认与默认互斥
    await request.post('/user/address', form.value)
    ElMessage.success('保存成功')
    dialog.value = false
    await load()
    return true
  } catch {
    return false
  } finally {
    saving.value = false
  }
}

async function saveAndReturn() {
  const ok = await save()
  if (ok) goBack()
}

async function setDefault(id) {
  try {
    await request.put('/user/address/default', null, { params: { id } })
    ElMessage.success('已设为默认地址')
    load()
  } catch {
    // 错误已在拦截器提示
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定删除该收货地址吗？', '提示', { type: 'warning' })
    await request.delete('/user/address', { params: { id } })
    ElMessage.success('已删除')
    load()
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div class="page-card">
    <PageHeader title="收货地址" icon="Location" fallback="/profile">
      <template #actions>
        <el-button type="primary" round @click="openAdd">新增地址</el-button>
      </template>
    </PageHeader>

    <div v-if="returnPath" class="return-banner">
      <span>编辑完成后可返回继续操作</span>
      <el-button type="primary" round size="small" @click="goBack">{{ backLabel }}</el-button>
    </div>

    <el-table v-if="list.length" :data="list" stripe>
      <el-table-column label="收货人" width="120">
        <template #default="{ row }">
          <span>{{ row.receiverName }}</span>
          <el-tag v-if="row.isDefault === 1" type="success" size="small" class="default-tag">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="地址">
        <template #default="{ row }">{{ row.province }}{{ row.city }}{{ row.district }}{{ row.detail }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.isDefault !== 1" link type="primary" @click="setDefault(row.id)">设为默认</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else description="还没有收货地址">
      <el-button type="primary" round @click="openAdd">添加地址</el-button>
      <el-button v-if="returnPath" round @click="goBack">{{ backLabel }}</el-button>
    </el-empty>

    <div v-if="list.length && returnPath" class="page-footer">
      <el-button round @click="goBack">{{ backLabel }}</el-button>
    </div>

    <el-dialog
      v-model="dialog"
      :title="form.id ? '编辑地址' : '新增地址'"
      width="480px"
      :close-on-click-modal="false"
      @close="closeDialog"
    >
      <el-form label-width="80px">
        <el-form-item label="收货人" required>
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="电话" required>
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="省">
          <el-input v-model="form.province" placeholder="如：广东省" />
        </el-form-item>
        <el-form-item label="市">
          <el-input v-model="form.city" placeholder="如：深圳市" />
        </el-form-item>
        <el-form-item label="区">
          <el-input v-model="form.district" placeholder="如：南山区" />
        </el-form-item>
        <el-form-item label="详细地址" required>
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="街道、门牌号等" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button v-if="returnPath" type="success" :loading="saving" @click="saveAndReturn">
          保存并返回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.default-tag { margin-left: 8px; }
.page-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--mall-border);
  text-align: center;
}
</style>
