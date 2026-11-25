<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { submitAlipayPay, syncPayStatus } from '../utils/pay'
import PageHeader from '../components/PageHeader.vue'
import OrderReviewDialog from '../components/OrderReviewDialog.vue'

const route = useRoute()
const order = ref(null)
const tracks = ref([])
const mapData = ref(null)
const loading = ref(true)
const paying = ref(false)
const reviewVisible = ref(false)
const statusMap = ['待付款', '待发货', '待收货', '已完成', '已取消', '售后中']

const mapContainer = ref(null)
let mapInstance = null

onMounted(loadOrder)

async function loadOrder() {
  loading.value = true
  const id = route.params.id
  try {
    // 详情、物流时间线、地图轨迹并行加载
    const [o, t, m] = await Promise.all([
      request.get('/order/detail', { params: { id } }),
      request.get('/order/logistics', { params: { orderId: id } }),
      request.get('/order/logistics/map', { params: { orderId: id } })
    ])
    order.value = o.data
    tracks.value = t.data || []
    mapData.value = m.data
    await nextTick()
    renderMap()
  } finally {
    loading.value = false
  }
}

/** 用高德 JS API 渲染物流地图：标记 4 个节点，路线用驾车路径规划贴高速 */
function renderMap() {
  const points = mapData.value?.points || []
  if (!points.length || !mapContainer.value) return
  // index.html 引入的全局 AMap；若 Key 未替换则跳过渲染，保留节点文字列表兜底
  const AMap = window.AMap
  if (!AMap) {
    console.warn('[OrderDetail] AMap 未加载，请确认 index.html 中的高德 Key 已替换')
    return
  }
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
  mapInstance = new AMap.Map(mapContainer.value, {
    zoom: 8,
    center: [points[0].lng, points[0].lat],
    mapStyle: 'amap://styles/whitesmoke'
  })

  // 先画节点 Marker
  points.forEach((p, i) => {
    const marker = new AMap.Marker({
      position: [p.lng, p.lat],
      title: p.name,
      label: { content: `${i + 1}. ${p.name}（${p.status}）`, direction: 'top' }
    })
    mapInstance.add(marker)
  })

  const straightPath = points.map(p => [p.lng, p.lat])
  const drawStraightLine = () => drawRoutePolyline(AMap, straightPath)

  // 只有 2 个以上节点才需要规划路线；Driving 插件未加载时回退直线
  if (straightPath.length < 2 || !AMap.Driving) {
    drawStraightLine()
    return
  }

  // 驾车路径规划：起点 = 首节点，终点 = 末节点，其余作为途经点（高速优先策略）
  // hideMarkers:true 不显示高德自带的起终点 Marker，节点 Marker 由我们自己画
  const driving = new AMap.Driving({ hideMarkers: true, policy: AMap.DrivingPolicy?.LEAST_TIME })
  const origin = straightPath[0]
  const destination = straightPath[straightPath.length - 1]
  const waypoints = straightPath.slice(1, -1)
  driving.search(origin, destination, { waypoints }, (status, result) => {
    if (status !== 'complete' || !result.routes || !result.routes.length) {
      // 规划失败（如 Key 未开通 WebService）→ 回退到直线
      console.warn('[OrderDetail] 驾车路径规划失败，回退直线：', result)
      drawStraightLine()
      return
    }
    // 拼接每一段 step 的贴路坐标，形成完整轨迹
    const roadPath = []
    result.routes[0].steps.forEach(step => {
      if (Array.isArray(step.path)) roadPath.push(...step.path)
    })
    drawRoutePolyline(AMap, roadPath.length ? roadPath : straightPath)
  })
}

/** 绘制路线并自动调整视野 */
function drawRoutePolyline(AMap, path) {
  if (!mapInstance || path.length < 2) return
  const polyline = new AMap.Polyline({
    path,
    strokeColor: '#3b82f6',
    strokeWeight: 5,
    strokeOpacity: 0.9,
    strokeStyle: 'solid',
    lineJoin: 'round',
    showDir: true
  })
  mapInstance.add(polyline)
  mapInstance.setFitView([polyline], false, [60, 60, 60, 60])
}

async function continuePay() {
  if (!order.value) return
  paying.value = true
  try {
    await submitAlipayPay(request, { orderNo: order.value.orderNo, payAmount: order.value.payAmount })
    ElMessage.success('已打开支付页')
  } catch (e) {
    ElMessage.error(e?.message || '发起支付失败')
  } finally {
    paying.value = false
  }
}

async function syncPayment() {
  if (!order.value) return
  paying.value = true
  try {
    await syncPayStatus(request, order.value.orderNo)
    ElMessage.success('支付状态已同步')
    await loadOrder()
  } catch {
    // 错误已在拦截器提示
  } finally {
    paying.value = false
  }
}

async function confirmArrival() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm('确认商品已送达吗？', '确认到货', { type: 'info' })
    await request.post('/order/confirm', null, { params: { id: order.value.id } })
    ElMessage.success('已确认到货')
    await loadOrder()
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div v-loading="loading" class="page-card">
    <PageHeader title="订单详情" icon="Document" fallback="/orders" back-text="返回订单列表" />

    <template v-if="order">
      <div class="summary">
        <div class="row"><span>订单号</span><strong>{{ order.orderNo }}</strong></div>
        <div class="row"><span>订单状态</span><strong>{{ statusMap[order.orderStatus] || '未知' }}</strong></div>
        <div class="row"><span>商品合计</span><strong>¥{{ order.totalAmount }}</strong></div>
        <div v-if="order.discountAmount > 0" class="row discount">
          <span>优惠券抵扣</span><strong>-¥{{ order.discountAmount }}</strong>
        </div>
        <div class="row">
          <span>运费</span>
          <strong v-if="order.freightAmount > 0">¥{{ order.freightAmount }}</strong>
          <strong v-else class="free-ship">免运费</strong>
        </div>
        <div class="row"><span>实付金额</span><strong class="price">¥{{ order.payAmount }}</strong></div>
        <div v-if="order.orderStatus === 0" class="pay-actions">
          <el-button type="primary" round :loading="paying" @click="continuePay">继续支付</el-button>
          <el-button round :loading="paying" @click="syncPayment">同步支付状态</el-button>
        </div>
        <div v-if="order.orderStatus === 2" class="pay-actions">
          <el-button type="primary" round @click="confirmArrival">确认到货</el-button>
        </div>
        <div v-if="order.orderStatus === 3" class="pay-actions">
          <el-button type="primary" round @click="reviewVisible = true">评价订单</el-button>
        </div>
        <div v-if="mapData?.logisticsNo" class="row">
          <span>物流单号</span><strong>{{ mapData.logisticsCompany }} {{ mapData.logisticsNo }}</strong>
        </div>
      </div>

      <h3 class="sub-title">物流轨迹</h3>
      <el-timeline v-if="tracks.length">
        <el-timeline-item v-for="(t, i) in tracks" :key="i" :timestamp="t.time">{{ t.desc }}</el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无物流信息" :image-size="80" />

      <h3 class="sub-title">物流地图（福州 → 厦门物流轨迹）</h3>
      <div v-if="mapData?.points?.length" class="map-panel">
        <div ref="mapContainer" class="map-container"></div>
        <div class="map-legend">
          <div v-for="(p, i) in mapData.points" :key="i" class="map-point">
            <span class="dot">{{ i + 1 }}</span>
            <div>
              <strong>{{ p.name }}</strong>
              <p>{{ p.status }}</p>
            </div>
          </div>
        </div>
        <p class="map-tip">基于高德地图 JS API 驾车路径规划渲染，路线沿真实高速路网（规划失败自动回退直线），节点为后端模拟数据。</p>
      </div>
      <el-empty v-else description="暂无地图轨迹" :image-size="80" />

      <OrderReviewDialog
        v-model:visible="reviewVisible"
        :order-id="order.id"
        @reviewed="loadOrder"
      />
    </template>
  </div>
</template>

<style scoped>
.summary {
  background: var(--mall-bg);
  border-radius: var(--mall-radius-sm);
  padding: 16px 20px;
  margin-bottom: 24px;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}
.row span { color: var(--mall-text-secondary); }
.row.discount strong { color: #ef4444; }
.free-ship { color: #16a34a; }
.price { color: #ef4444; font-size: 18px; }
.pay-actions {
  display: flex;
  gap: 12px;
  padding-top: 12px;
  margin-top: 4px;
}
.sub-title {
  font-size: 16px;
  font-weight: 600;
  margin: 24px 0 16px;
}
.map-panel {
  background: var(--mall-bg);
  border-radius: var(--mall-radius-sm);
  padding: 16px;
}
.map-container {
  width: 100%;
  height: 420px;
  border-radius: var(--mall-radius-sm);
  overflow: hidden;
  border: 1px solid var(--mall-border);
}
.map-legend {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.map-point {
  flex: 1 1 45%;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 8px 10px;
  background: #fff;
  border-radius: var(--mall-radius-sm);
  border: 1px solid var(--mall-border);
}
.dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--mall-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}
.map-point p { margin: 4px 0 0; color: var(--mall-text-secondary); font-size: 13px; }
.map-tip { margin-top: 12px; font-size: 12px; color: var(--mall-text-secondary); }
</style>
