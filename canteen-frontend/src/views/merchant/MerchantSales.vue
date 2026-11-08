<template>
  <div class="page-container sales-page">
    <!-- 顶部指标卡 -->
    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-icon" style="background: linear-gradient(135deg,#ffd6a8,#ff9d5c)">🍜</div>
        <div class="summary-body">
          <div class="summary-value">{{ summary.dishCount ?? 0 }}</div>
          <div class="summary-label">菜单总数</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon" style="background: linear-gradient(135deg,#bfe3ff,#6bb8ff)">💰</div>
        <div class="summary-body">
          <div class="summary-value">¥{{ fmt(summary.totalIncome) }}</div>
          <div class="summary-label">总收入</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon" style="background: linear-gradient(135deg,#cdeecb,#8fd694)">📋</div>
        <div class="summary-body">
          <div class="summary-value">{{ summary.orderCount ?? 0 }}</div>
          <div class="summary-label">订单数</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon" style="background: linear-gradient(135deg,#e5d4ff,#b79bf0)">👥</div>
        <div class="summary-body">
          <div class="summary-value">{{ summary.customerCount ?? 0 }}</div>
          <div class="summary-label">客户数</div>
        </div>
      </div>
    </div>

    <!-- 收入趋势 + 每日收入 -->
    <div class="chart-grid">
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-head">
            <span class="card-title">每日收入</span>
            <el-radio-group v-model="dailyDays" size="small" @change="loadDaily">
              <el-radio-button :value="7">近7天</el-radio-button>
              <el-radio-button :value="30">近一月</el-radio-button>
              <el-radio-button :value="180">近半年</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <EChart :option="dailyOption" height="300px" />
      </el-card>

      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-head">
            <span class="card-title">近 30 天菜品销量占比</span>
          </div>
        </template>
        <EChart :option="monthlyOption" height="300px" />
      </el-card>
    </div>

    <!-- 热销 TOP3 / BOTTOM3 -->
    <div class="chart-grid">
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-head"><span class="card-title">卖得好的菜（TOP3）</span></div>
        </template>
        <EChart :option="bestOption" height="280px" />
      </el-card>
      <el-card shadow="never" class="page-card">
        <template #header>
          <div class="card-head"><span class="card-title">卖得差的菜（BOTTOM3）</span></div>
        </template>
        <EChart :option="worstOption" height="280px" />
      </el-card>
    </div>

    <!-- 近 7 天各菜销量 -->
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-head"><span class="card-title">近 7 天各菜销量</span></div>
      </template>
      <EChart :option="weeklyOption" height="300px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import EChart from '@/components/EChart.vue'
import { getSalesOverview, getSalesSummary, getDailyIncome } from '@/api/sales'

const weekly = ref([])
const monthly = ref([])
const daily = ref([])
const dailyDays = ref(7)
const summary = reactive({ dishCount: 0, totalIncome: 0, orderCount: 0, customerCount: 0 })

const ORANGE = '#f97316'

function fmt(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const weeklyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 60 },
  xAxis: { type: 'category', data: weekly.value.map(d => d.dishName), axisLabel: { rotate: 30 } },
  yAxis: { type: 'value', name: '份' },
  series: [{
    type: 'bar',
    data: weekly.value.map(d => d.quantity),
    itemStyle: { color: ORANGE, borderRadius: [4, 4, 0, 0] },
    barMaxWidth: 32
  }]
}))

const monthlyOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 份（{d}%）' },
  legend: { orient: 'vertical', right: 10, top: 'center' },
  series: [{
    type: 'pie',
    radius: ['42%', '68%'],
    center: ['38%', '50%'],
    data: monthly.value.map(d => ({ name: d.dishName, value: d.quantity })),
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{c}份' }
  }]
}))

const dailyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 20, bottom: 40 },
  xAxis: { type: 'category', data: daily.value.map(d => d.saleDate), boundaryGap: false },
  yAxis: { type: 'value', name: '元' },
  series: [{
    type: 'line',
    smooth: true,
    data: daily.value.map(d => Number(d.amount)),
    itemStyle: { color: '#6bb8ff' },
    lineStyle: { width: 3 },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(107,184,255,0.35)' },
          { offset: 1, color: 'rgba(107,184,255,0.02)' }
        ]
      }
    }
  }]
}))

function rankOption(list, reverse) {
  const data = reverse ? [...list].reverse() : list
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 90, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', name: '份' },
    yAxis: { type: 'category', data: data.map(d => d.dishName) },
    series: [{
      type: 'bar',
      data: data.map(d => d.quantity),
      itemStyle: { color: reverse ? '#94a3b8' : '#e0353b', borderRadius: [0, 4, 4, 0] },
      barMaxWidth: 20
    }]
  }
}

const bestOption = computed(() => rankOption(monthly.value.slice(0, 3), false))
const worstOption = computed(() => rankOption(monthly.value.slice(-3), true))

async function loadData() {
  const [ov, sm] = await Promise.all([getSalesOverview(), getSalesSummary()])
  weekly.value = ov.data.weekly || []
  monthly.value = ov.data.monthly || []
  Object.assign(summary, sm.data || {})
  loadDaily()
}

async function loadDaily() {
  const res = await getDailyIncome(dailyDays.value)
  daily.value = res.data || []
}

onMounted(loadData)
</script>

<style scoped>
.sales-page { max-width: 1200px; margin: 0 auto; }

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  transition: transform .18s ease, box-shadow .18s ease;
}

.summary-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-sm); }

.summary-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}

.summary-value { font-size: 22px; font-weight: 700; color: var(--text-1); line-height: 1.1; }
.summary-label { font-size: 12px; color: var(--text-3); margin-top: 4px; }

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}

.card-head { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 14px; font-weight: 600; color: var(--text-1); }

@media (max-width: 900px) {
  .summary-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-grid { grid-template-columns: 1fr; }
}
</style>
