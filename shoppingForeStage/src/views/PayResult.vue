<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { syncPayStatus, extractAlipayReturnParams } from '../utils/pay'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const syncing = ref(false)
const syncOk = ref(false)
const syncError = ref('')

async function doSync() {
  const orderNo = route.query.out_trade_no ? String(route.query.out_trade_no) : ''
  const returnParams = extractAlipayReturnParams(route.query)
  syncing.value = true
  syncError.value = ''
  try {
    await syncPayStatus(request, orderNo, returnParams)
    syncOk.value = true
    ElMessage.success('支付状态已同步')
  } catch (e) {
    syncError.value = e?.message || '同步失败，请稍后在订单页点击「同步支付状态」'
  } finally {
    syncing.value = false
  }
}

onMounted(async () => {
  await cartStore.refreshCount()
  const orderNo = route.query.out_trade_no
  // 支付宝 returnUrl 回跳时带 out_trade_no，自动验签并更新订单
  if (orderNo) {
    await doSync()
  } else {
    syncOk.value = true
  }
  if (syncOk.value) {
    setTimeout(() => {
      router.replace(orderNo ? { path: '/orders', query: { paid: orderNo } } : '/orders')
    }, 1500)
  }
})

function goOrders() {
  const orderNo = route.query.out_trade_no
  router.replace(orderNo ? { path: '/orders', query: { paid: orderNo } } : '/orders')
}
</script>

<template>
  <div class="page-card pay-result">
    <el-result
      v-if="syncing"
      icon="info"
      title="正在确认支付结果"
      sub-title="请稍候，正在确认支付状态…"
    />
    <el-result
      v-else-if="syncOk"
      icon="success"
      title="支付已完成"
      sub-title="感谢您的购买，正在跳转到订单页…"
    >
      <template #extra>
        <el-button type="primary" round @click="goOrders">立即查看订单</el-button>
      </template>
    </el-result>
    <el-result
      v-else
      icon="warning"
      title="支付结果待确认"
      :sub-title="syncError || '未能自动同步支付状态'"
    >
      <template #extra>
        <el-button
          v-if="route.query.out_trade_no"
          type="primary"
          round
          :loading="syncing"
          @click="doSync"
        >
          重新确认
        </el-button>
        <el-button round @click="goOrders">查看订单</el-button>
        <el-button round @click="router.replace('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.pay-result {
  max-width: 560px;
  margin: 40px auto;
  text-align: center;
}
</style>
