<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { isValidPhone, isValidEmail, trimAuthFields } from '../../utils/authValidate'

function validateUserForm(form, { requirePassword = false } = {}) {
  const username = (form.username || '').trim()
  if (!username) {
    ElMessage.warning('请填写用户名')
    return false
  }
  if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]{2,20}$/.test(username)) {
    ElMessage.warning('用户名为2-20位，支持中文、字母、数字、下划线')
    return false
  }
  if (requirePassword && (!form.password || form.password.length < 6)) {
    ElMessage.warning('密码至少6位')
    return false
  }
  const phone = (form.phone || '').trim()
  const email = (form.email || '').trim()
  if (phone && !isValidPhone(phone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return false
  }
  if (email && !isValidEmail(email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return false
  }
  return true
}

function normalizeUserPayload(form, { includePassword = false } = {}) {
  const payload = trimAuthFields({
    id: form.id,
    username: form.username,
    phone: form.phone || '',
    email: form.email || '',
    role: form.role,
    status: form.status
  })
  if (!payload.phone) payload.phone = null
  if (!payload.email) payload.email = null
  if (includePassword) payload.password = form.password
  return payload
}

const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const editVisible = ref(false)
const createVisible = ref(false)
const editForm = ref({ id: null, username: '', phone: '', email: '', role: 0, status: 1 })
const createForm = ref({ username: '', password: '', phone: '', email: '', role: 0, status: 1 })

async function load() {
  const res = await request.get('/admin/users', {
    params: { page: page.value, size: size.value, keyword: keyword.value || undefined }
  })
  users.value = res.data.records || []
  total.value = res.data.total || 0
}

function openCreate() {
  createForm.value = { username: '', password: '', phone: '', email: '', role: 0, status: 1 }
  createVisible.value = true
}

async function createUser() {
  if (!validateUserForm(createForm.value, { requirePassword: true })) return
  await request.post('/admin/users', normalizeUserPayload(createForm.value, { includePassword: true }))
  ElMessage.success('用户已创建')
  createVisible.value = false
  load()
}

function openEdit(row) {
  editForm.value = {
    id: row.id,
    username: row.username,
    phone: row.phone,
    email: row.email,
    role: row.role ?? 0,
    status: row.status ?? 1
  }
  editVisible.value = true
}

async function saveUser() {
  if (!validateUserForm(editForm.value)) return
  await request.put('/admin/users', normalizeUserPayload(editForm.value))
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}

async function removeUser(id) {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？此操作不可恢复。', '删除用户', { type: 'warning' })
    await request.delete('/admin/users', { params: { id } })
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
      <h2>用户管理</h2>
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索用户名/手机/邮箱" clearable style="width:220px" @keyup.enter="page=1;load()" />
        <el-button @click="page=1;load()">查询</el-button>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="phone" label="手机" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
            {{ row.role === 1 ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑/角色</el-button>
          <el-button size="small" type="danger" @click="removeUser(row.id)">删除</el-button>
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

    <el-dialog v-model="createVisible" title="新增用户" width="460px">
      <el-form label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="createForm.username" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="createForm.password" type="password" show-password /></el-form-item>
        <el-form-item label="手机"><el-input v-model="createForm.phone" placeholder="选填，11位手机号" maxlength="11" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="createForm.email" placeholder="选填，如 user@example.com" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" style="width:100%">
            <el-option :value="0" label="普通用户" />
            <el-option :value="1" label="管理员" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="createForm.status" style="width:100%">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="createUser">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑用户 / 权限角色分配" width="460px">
      <el-form label-width="80px">
        <el-form-item label="用户名"><el-input v-model="editForm.username" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="editForm.phone" placeholder="留空表示不绑定手机" maxlength="11" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" placeholder="留空表示不绑定邮箱" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width:100%">
            <el-option :value="0" label="普通用户" />
            <el-option :value="1" label="管理员" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width:100%">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
