<script setup>
/**
 * AI 知识库管理（RAG）。
 * 录入的文本由后端自动切块、向量化并加入内存索引；
 * 用户在 AI 咨询页打开「知识库」开关后，回答会优先参考这些资料。
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const list = ref([])
const dialogVisible = ref(false)
const saving = ref(false)
const rebuilding = ref(false)
const form = ref({ title: '', content: '' })

async function load() {
  const res = await request.get('/ai/kb')
  list.value = res.data || []
}

function openAdd() {
  form.value = { title: '', content: '' }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.title.trim()) return ElMessage.warning('请填写标题')
  if (!form.value.content.trim()) return ElMessage.warning('请填写知识正文')
  saving.value = true
  try {
    // 后端返回提示：向量化失败（如 embedding 模型未开通）时原文仍会保留
    const res = await request.post('/ai/kb', form.value)
    ElMessage.success(res.data || '已保存')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除知识「${row.title}」？索引会同步更新。`, '提示', { type: 'warning' })
  await request.delete(`/ai/kb/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function rebuild() {
  rebuilding.value = true
  try {
    const res = await request.post('/ai/kb/rebuild')
    ElMessage.success(res.data?.ready ? '索引重建完成' : '索引未就绪，请检查向量化模型配置')
  } finally {
    rebuilding.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-card>
    <div class="toolbar">
      <h2>AI 知识库</h2>
      <div>
        <el-tooltip content="修改数据库或向量化模型刚开通后，手动全量重建内存向量索引" placement="bottom">
          <el-button :loading="rebuilding" @click="rebuild">重建索引</el-button>
        </el-tooltip>
        <el-button type="primary" @click="openAdd">新增知识</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="tip">
      录入商城规则、常见问答等文本，系统会自动分块向量化。用户开启「知识库」开关提问时，AI 将优先依据这些资料回答。
    </el-alert>

    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" width="240" show-overflow-tooltip />
      <el-table-column prop="content" label="内容摘要" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">{{ new Date(row.createTime).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增知识" width="640px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="如：退换货政策" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="粘贴知识原文，建议 200 字以上效果更好；系统会按约 400 字自动切块"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存并向量化</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.toolbar h2 { margin: 0; }
.tip { margin-bottom: 16px; }
</style>
