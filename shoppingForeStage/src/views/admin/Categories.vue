<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const categories = ref([])
const dialogVisible = ref(false)
const form = ref({ categoryId: null, categoryName: '' })

async function load() {
  const res = await request.get('/admin/categories')
  categories.value = res.data || []
}

function openAdd() {
  form.value = { categoryId: null, categoryName: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { categoryId: row.categoryId, categoryName: row.categoryName }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.categoryName?.trim()) return ElMessage.warning('请输入分类名称')
  await request.post('/admin/categories', form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('删除分类不会删除商品，但商品仍保留原分类 ID。确定删除？', '提示', { type: 'warning' })
    await request.delete('/admin/categories', { params: { id } })
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
      <h2>商品分类</h2>
      <el-button type="primary" @click="openAdd">新增分类</el-button>
    </div>
    <el-table :data="categories" stripe>
      <el-table-column prop="categoryId" label="分类 ID" width="100" />
      <el-table-column prop="categoryName" label="分类名称" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row.categoryId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.categoryId ? '编辑分类' : '新增分类'" width="400px">
      <el-form label-width="80px">
        <el-form-item v-if="form.categoryId" label="分类 ID">{{ form.categoryId }}</el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.categoryName" placeholder="如：数码、运动" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
