<script setup>
/**
 * 订单评价弹窗：按订单商品粒度评价，已完成订单每商品仅可评一次。
 * 父组件 v-model:visible + :order-id，@reviewed 刷新列表。
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const props = defineProps({
  orderId: { type: [Number, String], required: true }
})

const visible = defineModel('visible', { type: Boolean, default: false })
const emit = defineEmits(['reviewed'])

const loading = ref(false)
const submittingId = ref(null)
const items = ref([])
const forms = ref({})

async function loadItems() {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await request.get('/user/review/order-items', { params: { orderId: props.orderId } })
    items.value = res.data || []
    const nextForms = {}
    for (const item of items.value) {
      if (Number(item.reviewed) !== 1) {
        nextForms[item.productId] = { rating: 5, content: '' }
      }
    }
    forms.value = nextForms
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

watch(visible, (open) => {
  if (open) loadItems()
})

async function submit(item) {
  const form = forms.value[item.productId]
  if (!form?.content?.trim()) {
    return ElMessage.warning(`请填写「${item.productName}」的评价内容`)
  }
  submittingId.value = item.productId
  try {
    await request.post('/user/review', {
      orderId: Number(props.orderId),
      productId: item.productId,
      rating: form.rating,
      content: form.content.trim()
    })
    ElMessage.success('评价成功')
    emit('reviewed')
    await loadItems()
  } catch {
    // 错误已在拦截器提示
  } finally {
    submittingId.value = null
  }
}

const pendingCount = () => items.value.filter(i => Number(i.reviewed) !== 1).length
</script>

<template>
  <el-dialog
    v-model="visible"
    title="订单评价"
    width="560px"
    destroy-on-close
    @closed="items = []"
  >
    <div v-loading="loading">
      <p v-if="items.length" class="tip">仅已完成订单可评价，每个商品只能评价一次。</p>
      <el-empty v-if="!loading && !items.length" description="暂无可评价商品" />
      <div v-for="item in items" :key="item.orderItemId" class="review-block">
        <div class="product-head">
          <strong>{{ item.productName }}</strong>
          <el-tag v-if="Number(item.reviewed) === 1" type="success" size="small">已评价</el-tag>
          <el-tag v-else type="warning" size="small">待评价</el-tag>
        </div>
        <template v-if="Number(item.reviewed) === 1">
          <div class="reviewed">
            <el-rate :model-value="item.rating" disabled size="small" />
            <p>{{ item.content }}</p>
          </div>
        </template>
        <template v-else-if="forms[item.productId]">
          <el-rate v-model="forms[item.productId].rating" />
          <el-input
            v-model="forms[item.productId].content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="分享您的使用体验..."
          />
          <el-button
            type="primary"
            size="small"
            round
            :loading="submittingId === item.productId"
            @click="submit(item)"
          >
            提交评价
          </el-button>
        </template>
      </div>
    </div>
    <template #footer>
      <el-button round @click="visible = false">
        {{ pendingCount() ? '稍后评价' : '关闭' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.review-block {
  padding: 16px 0;
  border-bottom: 1px solid var(--mall-border, #ebeef5);
}
.review-block:last-child {
  border-bottom: none;
}
.product-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.reviewed p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.el-rate {
  margin-bottom: 10px;
}
.el-textarea {
  margin-bottom: 10px;
}
</style>
