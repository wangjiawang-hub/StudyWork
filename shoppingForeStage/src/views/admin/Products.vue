<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const products = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const categoryId = ref(null)
const dialogVisible = ref(false)
const form = ref({
  id: null, name: '', categoryId: null, categoryName: '', price: 0, stock: 0, status: 1, description: '', image: ''
})

// ---- SKU 规格配置 ----
const skuDialogVisible = ref(false)
const skuForm = ref({ productId: null, specs: [], skuList: [] })

async function loadCategories() {
  const res = await request.get('/admin/categories')
  categories.value = res.data || []
}

async function load() {
  const res = await request.get('/admin/products', {
    params: {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined
    }
  })
  products.value = res.data.records || []
  total.value = res.data.total || 0
}

function openAdd() {
  form.value = { id: null, name: '', categoryId: null, categoryName: '', price: 0, stock: 0, status: 1, description: '', image: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  dialogVisible.value = true
}

function onCategoryChange(id) {
  const cat = categories.value.find(c => c.categoryId === id)
  form.value.categoryName = cat?.categoryName || ''
}

async function save() {
  if (!form.value.name?.trim()) return ElMessage.warning('请输入商品名称')
  await request.post('/admin/products', form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定删除该商品吗？', '提示', { type: 'warning' })
    await request.delete('/admin/products', { params: { id } })
    ElMessage.success('已删除')
    load()
  } catch {
    // 用户取消
  }
}

// ---- SKU 管理：规格维度 + 组合表格 ----
function openSkuManager(row) {
  skuForm.value = { productId: row.id, productName: row.name, specs: [], skuList: [] }
  loadSkus(row.id)
  skuDialogVisible.value = true
}

async function loadSkus(productId) {
  const res = await request.get('/admin/skus', { params: { productId } })
  const skuList = res.data || []
  if (skuList.length) {
    // 从已有 SKU 的 specs 反推规格维度
    const specMap = new Map()
    for (const sku of skuList) {
      for (const spec of (sku.specs || [])) {
        if (!specMap.has(spec.specName)) {
          specMap.set(spec.specName, [])
        }
        const values = specMap.get(spec.specName)
        if (!values.includes(spec.specValue)) {
          values.push(spec.specValue)
        }
      }
    }
    skuForm.value.specs = Array.from(specMap.entries()).map(([name, values]) => ({ name, values }))
    skuForm.value.skuList = skuList.map(sku => ({
      id: sku.id,
      skuCode: sku.skuCode,
      price: sku.price,
      stock: sku.stock,
      specs: (sku.specs || []).map(sp => ({ specName: sp.specName, specValue: sp.specValue }))
    }))
  }
}

function addSpecRow() {
  skuForm.value.specs.push({ name: '', values: [] })
}

function removeSpecRow(index) {
  skuForm.value.specs.splice(index, 1)
  generateSkuList()
}

function addSpecValue(specRow, value) {
  if (!value?.trim()) return
  specRow.values = specRow.values || []
  if (!specRow.values.includes(value.trim())) {
    specRow.values.push(value.trim())
    generateSkuList()
  }
}

function removeSpecValue(specRow, index) {
  specRow.values.splice(index, 1)
  generateSkuList()
}

function generateSkuList() {
  const specs = skuForm.value.specs.filter(s => s.name && s.values.length)
  if (!specs.length) {
    skuForm.value.skuList = []
    return
  }
  // 笛卡尔积生成所有组合
  let combos = [{}]
  for (const spec of specs) {
    const next = []
    for (const existing of combos) {
      for (const value of spec.values) {
        next.push({ ...existing, [spec.name]: value })
      }
    }
    combos = next
  }
  // 保留旧值（价格/库存）如果组合没变
  const oldMap = new Map()
  for (const sku of skuForm.value.skuList) {
    const key = (sku.specs || []).map(s => s.specValue).join('|')
    oldMap.set(key, sku)
  }
  skuForm.value.skuList = combos.map(combo => {
    const specList = specs.map(s => ({ specName: s.name, specValue: combo[s.name] }))
    const key = specList.map(s => s.specValue).join('|')
    const old = oldMap.get(key)
    if (old) return old
    return { id: null, skuCode: '', price: 0, stock: 0, specs: specList }
  })
}

const specValueInput = ref({})

async function saveSkus() {
  if (!skuForm.value.skuList.length) {
    return ElMessage.warning('请先添加规格维度和值')
  }
  await request.post('/admin/skus', {
    productId: skuForm.value.productId,
    skus: skuForm.value.skuList
  })
  ElMessage.success('SKU保存成功')
  skuDialogVisible.value = false
  load()
}

async function deleteSku(id) {
  await request.delete('/admin/skus', { params: { id } })
  ElMessage.success('已删除')
  loadSkus(skuForm.value.productId)
}

onMounted(async () => {
  await loadCategories()
  await load()
})
</script>

<template>
  <el-card>
    <div class="toolbar">
      <h2>商品管理</h2>
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索商品名" clearable style="width:180px" @keyup.enter="page=1;load()" />
        <el-select v-model="categoryId" clearable placeholder="分类筛选" style="width:140px" @change="page=1;load()">
          <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
        </el-select>
        <el-button @click="page=1;load()">查询</el-button>
        <el-button type="primary" @click="openAdd">新增商品</el-button>
      </div>
    </div>

    <el-table :data="products" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="price" label="价格" width="90">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="success" @click="openSkuManager(row)">SKU</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
      <el-form label-width="80px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" style="width:100%" @change="onCategoryChange">
            <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="图片URL"><el-input v-model="form.image" placeholder="商品主图链接" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option :value="1" label="上架" />
            <el-option :value="0" label="下架" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- SKU 管理弹窗 -->
    <el-dialog v-model="skuDialogVisible" title="SKU 规格管理" width="800px">
      <div class="sku-section">
        <div class="sku-section-header">
          <h4>规格维度</h4>
          <el-button size="small" type="primary" @click="addSpecRow">+ 添加规格</el-button>
        </div>
        <div v-for="(spec, idx) in skuForm.specs" :key="idx" class="spec-row">
          <el-input v-model="spec.name" placeholder="规格名(如:颜色)" style="width:120px" @change="generateSkuList" />
          <div class="spec-values">
            <el-tag
              v-for="(val, vi) in spec.values"
              :key="vi"
              closable
              @close="removeSpecValue(spec, vi)"
              style="margin-right:6px"
            >{{ val }}</el-tag>
            <el-input
              v-if="spec.name"
              v-model="specValueInput[idx]"
              placeholder="+ 规格值"
              size="small"
              style="width:120px"
              @keyup.enter="addSpecValue(spec, specValueInput[idx]); specValueInput[idx] = ''"
            />
          </div>
          <el-button size="small" type="danger" link @click="removeSpecRow(idx)">删除</el-button>
        </div>
        <el-empty v-if="!skuForm.specs.length" description="点击添加规格维度" :image-size="40" />
      </div>

      <div v-if="skuForm.skuList.length" class="sku-section">
        <div class="sku-section-header">
          <h4>SKU 组合（{{ skuForm.skuList.length }} 个）</h4>
          <el-button size="small" type="primary" @click="saveSkus">保存全部SKU</el-button>
        </div>
        <el-table :data="skuForm.skuList" stripe size="small" max-height="300">
          <el-table-column
            v-for="spec in skuForm.specs.filter(s => s.name)"
            :key="spec.name"
            :label="spec.name"
            min-width="100"
          >
            <template #default="{ row }">
              {{ row.specs.find(s => s.specName === spec.name)?.specValue || '' }}
            </template>
          </el-table-column>
          <el-table-column label="编码" width="140">
            <template #default="{ row }">
              <el-input v-model="row.skuCode" size="small" placeholder="SKU编码" />
            </template>
          </el-table-column>
          <el-table-column label="价格" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button v-if="row.id" size="small" type="danger" link @click="deleteSku(row.id)">删</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
.sku-section { margin-bottom: 20px; }
.sku-section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.sku-section-header h4 { font-size: 15px; margin: 0; }
.spec-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.spec-values { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; flex: 1; }
</style>
