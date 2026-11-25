<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { submitAlipayPay } from '../utils/pay'
import { useCartStore } from '../stores/cart'
import { useMallStore } from '../stores/mall'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const cartStore = useCartStore()
const mallStore = useMallStore()
const addresses = ref([])
const coupons = ref([])
const cartItems = ref([])
const addressId = ref(null)
const userCouponId = ref(null)
const remark = ref('')
const paying = ref(false)
/** 从购物车页写入的待结算 cart 主键 ID 列表 */
const cartIds = JSON.parse(localStorage.getItem('checkoutCartIds') || '[]')

/** 选中商品金额合计（预览用，最终以服务端 /order/create 返回为准） */
const orderTotal = computed(() =>
  cartItems.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0)
)

const selectedCoupon = computed(() =>
  coupons.value.find(c => c.id === userCouponId.value)
)

const discountAmount = computed(() => {
  const c = selectedCoupon.value?.coupon
  if (!c || orderTotal.value < Number(c.threshold || 0)) return 0
  if (c.type === 0) return Number(c.reduceAmount || 0) // 满减券
  const rate = Number(c.discountRate || 1)
  let d = orderTotal.value * (1 - rate) // 折扣券
  if (c.maxDiscount != null && d > Number(c.maxDiscount)) d = Number(c.maxDiscount)
  return Math.round(d * 100) / 100
})

const freightAmount = computed(() => mallStore.calcFreight(orderTotal.value))

const payAmount = computed(() =>
  Math.max(0, Math.round((orderTotal.value - discountAmount.value + freightAmount.value) * 100) / 100)
)

function isCouponEligible(c) {
  const threshold = Number(c.coupon?.threshold || 0)
  return orderTotal.value >= threshold
}

function couponLabel(c) {
  const name = c.coupon?.name || '优惠券'
  const threshold = c.coupon?.threshold
  const reduce = c.coupon?.reduceAmount
  const suffix = threshold != null ? ` (满${threshold}可用)` : ''
  const off = reduce != null ? ` -¥${reduce}` : ''
  if (!isCouponEligible(c)) return `${name}${suffix}${off} — 未满足条件`
  return `${name}${suffix}${off}`
}

async function loadCheckoutData() {
  const [addr, cps, cartRes] = await Promise.all([
    request.get('/user/address/list'),
    request.get('/coupon/my'),
    request.get('/cart/list')
  ])
  addresses.value = addr.data || []
  coupons.value = (cps.data || []).filter(c => c.status === 0)
  const allCarts = cartRes.data || []
  cartItems.value = allCarts.filter(c => cartIds.includes(c.id))

  if (!cartItems.value.length) {
    ElMessage.warning('结算商品不存在或已失效，请重新选择')
    router.replace('/cart')
    return
  }

  if (addresses.value.length) {
    const defaultAddr = addresses.value.find(a => a.isDefault === 1)
    const exists = addresses.value.some(a => a.id === addressId.value)
    if (!exists) {
      addressId.value = defaultAddr ? defaultAddr.id : addresses.value[0].id
    }
  } else {
    addressId.value = null
  }

  if (userCouponId.value && !isCouponEligible(coupons.value.find(c => c.id === userCouponId.value))) {
    userCouponId.value = null
  }
}

onMounted(loadCheckoutData)

watch(userCouponId, id => {
  if (!id) return
  const c = coupons.value.find(x => x.id === id)
  if (c && !isCouponEligible(c)) {
    userCouponId.value = null
    ElMessage.warning(`订单未满${c.coupon?.threshold}元，无法使用该优惠券`)
  }
})

function goManageAddress() {
  router.push({ path: '/address', query: { from: '/checkout' } })
}

async function submit() {
  if (!cartItems.value.length) return ElMessage.warning('结算商品为空')
  if (!addresses.value.length) return ElMessage.warning('请先添加收货地址')
  if (!addressId.value) return ElMessage.warning('请选择收货地址')
  if (userCouponId.value && !isCouponEligible(selectedCoupon.value)) {
    return ElMessage.warning(`订单未满${selectedCoupon.value?.coupon?.threshold}元，无法使用该优惠券`)
  }

  paying.value = true
  try {
    const res = await request.post('/order/create', {
      addressId: addressId.value,
      cartIds,
      userCouponId: userCouponId.value,
      remark: remark.value
    })
    localStorage.removeItem('checkoutCartIds')
    await cartStore.refreshCount()
    // 支付金额必须用后端返回值，防止前端预览与核销结果不一致
    await submitAlipayPay(request, { orderNo: res.data.orderNo, payAmount: res.data.payAmount })
    ElMessage.success('已打开支付宝支付页，支付完成后将自动跳回商城')
  } catch (e) {
    ElMessage.error(e?.message || '下单或支付失败')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <div class="page-card checkout-page">
    <PageHeader title="订单结算" icon="Wallet" fallback="/cart" back-text="返回购物车" />

    <el-alert
      v-if="!addresses.length"
      type="warning"
      show-icon
      :closable="false"
      title="您还没有收货地址，请先添加后再结算"
      class="addr-alert"
    >
      <el-button type="primary" size="small" round @click="goManageAddress">去添加地址</el-button>
    </el-alert>

    <div v-if="cartItems.length" class="order-summary">
      <h4>结算商品</h4>
      <div v-for="item in cartItems" :key="item.id" class="summary-item">
        <span>{{ item.productName }}<template v-if="item.skuSpec"> ({{ item.skuSpec }})</template> × {{ item.quantity }}</span>
        <span>¥{{ (item.price * item.quantity).toFixed(2) }}</span>
      </div>
      <div class="summary-row">
        <span>商品合计</span>
        <span>¥{{ orderTotal.toFixed(2) }}</span>
      </div>
      <div v-if="discountAmount > 0" class="summary-row discount">
        <span>优惠券抵扣</span>
        <span>-¥{{ discountAmount.toFixed(2) }}</span>
      </div>
      <div class="summary-row">
        <span>运费</span>
        <span v-if="freightAmount > 0">¥{{ freightAmount.toFixed(2) }}</span>
        <span v-else class="free-ship">免运费</span>
      </div>
      <p v-if="freightAmount > 0" class="ship-tip">
        再买 ¥{{ Math.max(0, mallStore.freeShippingAmount - orderTotal).toFixed(2) }} 即可{{ mallStore.freeShippingTip }}
      </p>
      <div class="summary-row total">
        <span>应付金额</span>
        <strong>¥{{ payAmount.toFixed(2) }}</strong>
      </div>
    </div>

    <el-form label-width="100px" size="large" class="checkout-form">
      <el-form-item label="收货地址">
        <el-select
          v-model="addressId"
          placeholder="请选择收货地址"
          style="width:100%"
          :disabled="!addresses.length"
        >
          <el-option
            v-for="a in addresses"
            :key="a.id"
            :label="`${a.isDefault === 1 ? '[默认] ' : ''}${a.receiverName} ${a.phone} ${a.province}${a.city}${a.detail}`"
            :value="a.id"
          />
        </el-select>
        <el-button link type="primary" @click="goManageAddress">管理地址</el-button>
      </el-form-item>
      <el-form-item label="优惠券">
        <el-select
          v-model="userCouponId"
          clearable
          placeholder="选择优惠券（可选）"
          style="width:100%"
        >
          <el-option
            v-for="c in coupons"
            :key="c.id"
            :label="couponLabel(c)"
            :value="c.id"
            :disabled="!isCouponEligible(c)"
          />
        </el-select>
        <el-button link type="primary" @click="router.push('/coupon')">去领券</el-button>
        <p v-if="userCouponId && !isCouponEligible(selectedCoupon)" class="coupon-warn">
          当前订单未满 {{ selectedCoupon?.coupon?.threshold }} 元，无法使用该券
        </p>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="remark" placeholder="给卖家留言（选填）" />
      </el-form-item>
      <el-form-item>
        <div class="actions">
          <el-button type="primary" size="large" round :loading="paying" :disabled="!addresses.length || !cartItems.length" @click="submit">
            <el-icon><CreditCard /></el-icon> 提交并跳转支付宝
          </el-button>
          <el-button size="large" round @click="router.push('/cart')">返回购物车</el-button>
        </div>
        <p class="tip">
          <el-icon><InfoFilled /></el-icon>
          支付完成后将跳回商城并自动确认订单状态；若沙箱繁忙，可在订单页点击「同步支付状态」
        </p>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.checkout-form {
  max-width: 640px;
}
.addr-alert {
  margin-bottom: 20px;
}
.order-summary {
  background: var(--mall-bg);
  border-radius: var(--mall-radius-sm);
  padding: 16px 20px;
  margin-bottom: 24px;
}
.order-summary h4 {
  margin-bottom: 12px;
  font-size: 15px;
}
.summary-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--mall-text-secondary);
  padding: 6px 0;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px dashed var(--mall-border);
  font-size: 14px;
}
.summary-row.discount {
  color: #ef4444;
}
.free-ship {
  color: #16a34a;
  font-weight: 500;
}
.ship-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--mall-text-secondary);
}
.summary-row.total strong {
  color: #ef4444;
  font-size: 20px;
}
.coupon-warn {
  margin-top: 6px;
  font-size: 12px;
  color: #ef4444;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.tip {
  margin-top: 16px;
  font-size: 13px;
  color: var(--mall-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.5;
}
</style>
