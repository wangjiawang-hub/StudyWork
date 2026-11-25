<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { submitAlipayPay, syncPayStatus } from '../utils/pay'
import { useCartStore } from '../stores/cart'
import PageHeader from '../components/PageHeader.vue'
import OrderReviewDialog from '../components/OrderReviewDialog.vue'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const orders = ref([])
const payingId = ref(null)
const reviewOrderId = ref(null)
const reviewVisible = ref(false)
const statusMap = ['待付款', '待发货', '待收货', '已完成', '已取消', '售后中']

async function load() {
  const res = await request.get('/order/list')
  orders.value = res.data || []
}

onMounted(async () => {
  await cartStore.refreshCount()
  // PayResult 跳转可能已同步，此处再 sync 一次兜底
  if (route.query.paid) {
    try {
      await syncPayStatus(request, String(route.query.paid))
    } catch {
      // PayResult 可能已同步，此处作为兜底
    }
  }
  await load()
  if (route.query.paid) {
    ElMessage.success('支付成功，订单状态已更新')
    router.replace('/orders')
  }
})

async function cancel(id) {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    await request.post('/order/cancel', null, { params: { id } })
    ElMessage.success('已取消')
    load()
  } catch { /* 用户取消 */ }
}

async function confirm(id) {
  try {
    await ElMessageBox.confirm('确认商品已送达吗？', '确认到货', { type: 'info' })
    await request.post('/order/confirm', null, { params: { id } })
    ElMessage.success('已确认到货')
    load()
  } catch { /* 用户取消 */ }
}

async function afterSale(id) {
  try {
    await ElMessageBox.confirm('确定申请售后吗？', '申请售后', { type: 'warning' })
    await request.post('/order/afterSale', null, { params: { id } })
    ElMessage.success('售后申请已提交')
  } catch { /* 用户取消 */ }
}

async function continuePay(order) {
  payingId.value = order.id
  try {
    await submitAlipayPay(request, { orderNo: order.orderNo, payAmount: order.payAmount })
    ElMessage.success('已打开支付页，完成支付后将自动跳转回商城')
  } catch (e) {
    ElMessage.error(e?.message || '发起支付失败')
  } finally {
    payingId.value = null
  }
}

async function syncPayment(order) {
  payingId.value = order.id
  try {
    await syncPayStatus(request, order.orderNo)
    ElMessage.success('支付状态已同步')
    load()
  } catch {
    // 错误已在拦截器提示
  } finally {
    payingId.value = null
  }
}

function openReview(order) {
  reviewOrderId.value = order.id
  reviewVisible.value = true
}
</script>

<template>
  <div class="page-card">
    <PageHeader title="我的订单" icon="List" fallback="/" />
    <el-empty v-if="!orders.length" description="暂无订单">
      <el-button type="primary" round @click="router.push('/products')">去逛逛</el-button>
    </el-empty>
    <div v-for="o in orders" :key="o.id" class="order">
      <div class="head">
        <span>订单号：{{ o.orderNo }}</span>
        <span>{{ statusMap[o.orderStatus] || '未知' }}</span>
      </div>
      <div v-for="item in o.items" :key="item.id" class="item">
        {{ item.productName }} x {{ item.quantity }} — ¥{{ item.totalPrice }}
      </div>
      <div class="foot">
        <span>实付：¥{{ o.payAmount }}</span>
        <div class="btns">
          <el-button size="small" @click="router.push({ path: `/order/${o.id}`, query: { from: '/orders' } })">详情</el-button>
          <el-button
            v-if="o.orderStatus === 0"
            size="small"
            type="primary"
            :loading="payingId === o.id"
            @click="continuePay(o)"
          >
            继续支付
          </el-button>
          <el-button
            v-if="o.orderStatus === 0"
            size="small"
            :loading="payingId === o.id"
            @click="syncPayment(o)"
          >
            同步支付状态
          </el-button>
          <el-button v-if="o.orderStatus === 0" size="small" @click="cancel(o.id)">取消</el-button>
          <el-button v-if="o.orderStatus === 2" size="small" type="primary" @click="confirm(o.id)">确认到货</el-button>
          <el-button v-if="o.orderStatus === 3" size="small" type="primary" @click="openReview(o)">评价</el-button>
          <el-button v-if="o.orderStatus === 3" size="small" @click="afterSale(o.id)">申请售后</el-button>
        </div>
      </div>
    </div>

    <OrderReviewDialog
      v-if="reviewOrderId"
      v-model:visible="reviewVisible"
      :order-id="reviewOrderId"
      @reviewed="load"
    />
  </div>
</template>

<style scoped>
.order {
  border: 1px solid var(--mall-border);
  border-radius: var(--mall-radius-sm);
  padding: 20px;
  margin-bottom: 16px;
  background: var(--mall-bg);
  transition: box-shadow 0.2s;
}
.order:hover { box-shadow: var(--mall-shadow); }
.head, .foot { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
.head span:last-child {
  font-size: 13px;
  font-weight: 600;
  color: var(--mall-primary);
  background: var(--el-color-primary-light-9);
  padding: 4px 12px;
  border-radius: 12px;
}
.item { padding: 8px 0; color: var(--mall-text-secondary); font-size: 14px; }
.foot { margin-top: 12px; border-top: 1px solid var(--mall-border); padding-top: 12px; }
.foot > span { font-weight: 600; color: #ef4444; }
.btns { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
</style>
