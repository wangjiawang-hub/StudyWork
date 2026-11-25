<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const stats = ref({})
// 订单状态码 -> 文案
const ORDER_STATUS = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消' }

const cards = ref([])

// 图表实例与容器引用
const trendRef = ref(null)
const categoryRef = ref(null)
const statusRef = ref(null)
const topRef = ref(null)
let charts = []

function initChart(el, option) {
  const chart = echarts.init(el)
  chart.setOption(option)
  charts.push(chart)
}

function fmtMoney(n) {
  return Number(n || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

async function load() {
  const res = await request.get('/admin/stats')
  stats.value = res.data || {}
  cards.value = [
    { label: '用户总数', value: stats.value.userCount ?? 0, icon: '👥', color: '#3b82f6' },
    { label: '在售商品', value: stats.value.productCount ?? 0, icon: '📦', color: '#10b981' },
    { label: '订单总数', value: stats.value.orderCount ?? 0, icon: '🧾', color: '#f59e0b' },
    { label: '累计销售额', value: '¥' + fmtMoney(stats.value.salesTotal), icon: '💰', color: '#ef4444' },
    { label: '优惠券领取', value: stats.value.couponReceived ?? 0, icon: '🎟️', color: '#8b5cf6' },
    { label: '优惠券使用', value: stats.value.couponUsed ?? 0, icon: '✅', color: '#06b6d4' }
  ]

  await nextTick()
  renderCharts()
}

function renderCharts() {
  charts.forEach(c => c.dispose())
  charts = []

  // 近7天订单量/销售额趋势（双 Y 轴）
  const trend = stats.value.orderTrend || []
  initChart(trendRef.value, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单量', '销售额'] },
    grid: { left: 50, right: 55, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trend.map(i => i.name) },
    yAxis: [
      { type: 'value', name: '订单量', minInterval: 1 },
      { type: 'value', name: '销售额(¥)' }
    ],
    series: [
      { name: '订单量', type: 'bar', data: trend.map(i => i.orders), itemStyle: { color: '#3b82f6' }, barMaxWidth: 30 },
      { name: '销售额', type: 'line', yAxisIndex: 1, data: trend.map(i => i.amount), smooth: true, itemStyle: { color: '#ef4444' } }
    ]
  })

  // 商品分类分布（饼图）
  const category = (stats.value.categoryDist || []).map(i => ({ name: i.name, value: i.value }))
  initChart(categoryRef.value, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'],
      data: category, label: { formatter: '{b}: {c}' }
    }]
  })

  // 订单状态分布（环形图）
  const status = (stats.value.orderStatusDist || []).map(i => ({ name: ORDER_STATUS[i.name] ?? i.name, value: i.value }))
  initChart(statusRef.value, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: '60%', center: ['50%', '45%'],
      data: status, label: { formatter: '{b}: {c}' }
    }]
  })

  // 热销商品 Top5（横向柱状图）
  const top = (stats.value.topProducts || []).slice().reverse()
  initChart(topRef.value, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 130, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: top.map(i => i.name) },
    series: [{ type: 'bar', data: top.map(i => i.value), itemStyle: { color: '#10b981' }, barMaxWidth: 22 }]
  })
}

function handleResize() {
  charts.forEach(c => c.resize())
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<template>
  <div>
    <!-- 核心指标卡片 -->
    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.label" :xs="12" :sm="8" :md="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" :style="{ background: c.color }">{{ c.icon }}</div>
          <div>
            <div class="stat-value">{{ c.value }}</div>
            <div class="stat-label">{{ c.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近7天趋势 + 热销 -->
    <el-row :gutter="16" class="mt">
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <h2>近 7 天订单 / 销售趋势</h2>
          <div ref="trendRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <h2>热销商品 Top5</h2>
          <div ref="topRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 分布统计 -->
    <el-row :gutter="16" class="mt">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <h2>商品分类分布</h2>
          <div ref="categoryRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <h2>订单状态分布</h2>
          <div ref="statusRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;
}
.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}
.stat-label {
  font-size: 12px;
  color: #94a3b8;
}
.mt {
  margin-top: 16px;
}
.chart {
  height: 320px;
}
</style>
