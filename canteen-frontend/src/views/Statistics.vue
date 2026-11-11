<template>
  <div class="page-container">
    <!-- 窗口广告轮播 -->
    <BannerCarousel />

    <!-- 总览指标卡 -->
    <el-row :gutter="14" style="margin-bottom: 18px">
      <el-col v-for="item in overviewCards" :key="item.label" :xs="12" :sm="8" :md="4">
        <div class="metric-card">
          <div class="metric-icon" :style="{ background: item.bg }">
            <img :src="item.icon" :alt="item.label" class="metric-icon-img">
          </div>
          <div class="metric-body">
            <div class="metric-value">{{ item.value }}</div>
            <div class="metric-label">{{ item.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 美食红黑榜：单价 × 评分散点 -->
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">🍜 美食红黑榜</span>
          <span class="muted" style="font-size: 12px">
            横轴为菜品单价，纵轴为平均评分，气泡越大代表评价人数越多
          </span>
        </div>
      </template>
      <ScatterBoard mode="dish" :data="dishScatterList" height="400px" />
    </el-card>

    <!-- 优质 / 劣质档口 TOP3 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="12">
        <el-card shadow="never" class="page-card rank-card rank-good" style="height: 100%">
          <template #header><span style="font-weight: 600">🌟 优质档口 TOP3</span></template>
          <div v-for="(s, i) in qualityTop" :key="s.stallId" class="rank-row">
            <span class="rank-medal" :style="{ background: MEDAL[i] }">{{ i + 1 }}</span>
            <div class="rank-main">
              <div class="rank-title">{{ s.stallName }} <span class="muted">{{ s.windowNo }} 号</span></div>
              <div class="rank-sub">
                学生 {{ Number(s.avgRating).toFixed(2) }} 分 · 卫生 {{ Number(s.hygieneScore).toFixed(1) }} 分
              </div>
            </div>
            <span class="rank-score good">{{ s.composite }}</span>
          </div>
          <el-empty v-if="!qualityTop.length" description="暂无评分卫生双高的档口" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="page-card rank-card rank-bad" style="height: 100%">
          <template #header><span style="font-weight: 600">⚠️ 劣质档口 TOP3</span></template>
          <div v-for="(s, i) in badTop" :key="s.stallId" class="rank-row">
            <span class="rank-medal" :style="{ background: DARK[i] }">{{ i + 1 }}</span>
            <div class="rank-main">
              <div class="rank-title">
                {{ s.stallName }} <span class="muted">{{ s.windowNo }} 号</span>
                <el-tag v-if="Number(s.hygieneScore) < 80" size="small" type="danger" effect="dark" round>
                  待整改
                </el-tag>
              </div>
              <div class="rank-sub">
                学生 {{ Number(s.avgRating).toFixed(2) }} 分 · 卫生 {{ Number(s.hygieneScore).toFixed(1) }} 分
              </div>
            </div>
            <span class="rank-score bad">{{ s.composite }}</span>
          </div>
          <el-empty v-if="!badTop.length" description="暂无评分卫生双低的档口" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表一：菜品平均分排行 -->
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">🏆 菜品平均分排行榜</span>
          <el-radio-group v-model="rankLimit" size="small" @change="loadAll">
            <el-radio-button :value="10">TOP 10</el-radio-button>
            <el-radio-button :value="20">TOP 20</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <EChart :option="rankOption" height="420px" />
    </el-card>

    <!-- 图表二 + 图表三 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="14">
        <el-card shadow="never" class="page-card" style="height: 100%">
          <template #header><span style="font-weight: 600">📊 各窗口好评占比</span></template>
          <EChart :option="goodRateOption" height="380px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="page-card" style="height: 100%">
          <template #header><span style="font-weight: 600">🔥 最受欢迎档口 TOP3</span></template>
          <EChart :option="topOption" height="250px" />
          <div v-for="item in topList" :key="item.stallId" style="margin-bottom: 10px">
            <div style="display: flex; justify-content: space-between; font-size: 13px">
              <span>
                <el-tag :type="medalType(item.rank)" size="small" effect="dark" round>
                  No.{{ item.rank }}
                </el-tag>
                {{ item.stallName }}
              </span>
              <span class="muted">{{ item.popularityScore }} 分</span>
            </div>
            <el-progress
              :percentage="toPercent(item.popularityScore)"
              :color="medalColor(item.rank)"
              :show-text="false"
              :stroke-width="8"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 综合档口评价：学生评分 × 卫生分（管理员） -->
    <el-card v-if="userStore.isAdmin" shadow="never" class="page-card" style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">🏪 综合档口评价</span>
          <span class="muted" style="font-size: 12px">
            横轴为学生评分，纵轴为学校评定卫生分，右上为优质区、左下为劣质区
          </span>
        </div>
      </template>
      <ScatterBoard mode="stall" :data="stallScatterList" height="420px" />
    </el-card>

    <!-- 图表四：评分趋势 -->
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-weight: 600">📈 近期评分与评价量趋势</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="14">近 14 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <EChart :option="trendOption" height="320px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import EChart from '@/components/EChart.vue'
import BannerCarousel from '@/components/BannerCarousel.vue'
import ScatterBoard from '@/components/ScatterBoard.vue'
import { useUserStore } from '@/store/user'
import {
  getOverview, getDishRanking, getStallGoodRate, getStallTop, getRatingTrend,
  getDishScatter, getStallScatter
} from '@/api/stats'

const userStore = useUserStore()
const overview = ref({})
const rankList = ref([])
const goodRateList = ref([])
const topList = ref([])
const trendList = ref([])
const dishScatterList = ref([])
const stallScatterList = ref([])
const rankLimit = ref(10)
const trendDays = ref(7)

const MEDAL = ['linear-gradient(135deg,#fbbf24,#f59e0b)', 'linear-gradient(135deg,#cbd5e1,#94a3b8)', 'linear-gradient(135deg,#fdba74,#ea8c37)']
const DARK = ['linear-gradient(135deg,#4b5563,#1f2937)', 'linear-gradient(135deg,#6b7280,#374151)', 'linear-gradient(135deg,#9ca3af,#4b5563)']

const qualityTop = computed(() =>
  stallScatterList.value.filter(s => s.tag === 'quality').slice(0, 3))

const badTop = computed(() =>
  stallScatterList.value.filter(s => s.tag === 'bad').sort((a, b) => a.composite - b.composite).slice(0, 3))

const overviewCards = computed(() => [
  { label: '食堂窗口', value: overview.value.stallCount ?? 0, icon: '/ui/emoji/stall.png', bg: 'linear-gradient(135deg,#fff1e6,#ffe3cc)' },
  { label: '在售菜品', value: overview.value.dishCount ?? 0, icon: '/ui/emoji/dish.png', bg: 'linear-gradient(135deg,#e8f4ff,#cfe6ff)' },
  { label: '累计评价', value: overview.value.reviewCount ?? 0, icon: '/ui/emoji/review.png', bg: 'linear-gradient(135deg,#eaf8ec,#d0ecd4)' },
  { label: '今日评价', value: overview.value.todayReviewCount ?? 0, icon: '/ui/emoji/today.png', bg: 'linear-gradient(135deg,#fff8e1,#ffefbf)' },
  { label: '新增收藏', value: overview.value.favoriteCount ?? 0, icon: '/ui/emoji/favorite.png', bg: 'linear-gradient(135deg,#f3eefe,#e3d8ff)' },
  { label: '投诉/留言', value: overview.value.feedbackCount ?? 0, icon: '/ui/emoji/feedback.png', bg: 'linear-gradient(135deg,#fdeef0,#fbd6db)' }
])

const rankOption = computed(() => {
  const data = [...rankList.value].reverse()
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const p = params[0]
        const raw = rankList.value.find(i => i.dishName === p.name) || {}
        return `${p.name}<br/>所属档口：${raw.stallName || '-'}（${raw.stallZone || '-'}）<br/>平均分：${p.value}<br/>评价数：${raw.reviewCount || 0} 条<br/>好评率：${raw.goodRate || 0}%`
      }
    },
    grid: { left: 110, right: 60, top: 10, bottom: 30 },
    xAxis: { type: 'value', min: 0, max: 5, splitLine: { lineStyle: { color: '#f0f1f3' } } },
    yAxis: {
      type: 'category',
      data: data.map(i => i.dishName),
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        type: 'bar',
        data: data.map(i => i.avgRating),
        barWidth: 16,
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#ffb066' },
              { offset: 1, color: '#ff6a00' }
            ]
          }
        },
        label: { show: true, position: 'right', formatter: '{c}', color: '#e35f00' }
      }
    ]
  }
})

const goodRateOption = computed(() => {
  const names = goodRateList.value.map(i => `${i.windowNo}号\n${i.stallName}`)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const raw = goodRateList.value.find(i => i.stallName === params[0].name.split('\n')[1]) || {}
        return `${raw.stallName || '-'}（${raw.stallZone || '-'} · ${raw.windowNo || ''}号）<br/>` +
          params.map(p => `${p.marker}${p.seriesName}：${p.value} 条`).join('<br/>') +
          `<br/>好评率：${raw.goodRate || 0}%`
      }
    },
    legend: { data: ['好评(4-5星)', '中评(3星)', '差评(1-2星)'], top: 0 },
    grid: { left: 40, right: 20, top: 40, bottom: 60 },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { interval: 0, fontSize: 11, lineHeight: 14 }
    },
    yAxis: { type: 'value', name: '评价数', splitLine: { lineStyle: { color: '#f0f1f3' } } },
    series: [
      { name: '好评(4-5星)', type: 'bar', stack: 'total', data: goodRateList.value.map(i => i.goodCount), itemStyle: { color: '#34c759' } },
      { name: '中评(3星)', type: 'bar', stack: 'total', data: goodRateList.value.map(i => i.normalCount), itemStyle: { color: '#ffa94d' } },
      { name: '差评(1-2星)', type: 'bar', stack: 'total', data: goodRateList.value.map(i => i.badCount), itemStyle: { color: '#ef4444' } }
    ]
  }
})

const topOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: p => {
      const raw = topList.value.find(i => i.stallName === p.name) || {}
      return `${p.name}（${raw.stallZone || '-'}）<br/>热度分：${p.value}`
    }
  },
  legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
  series: [
    {
      type: 'pie',
      radius: ['32%', '62%'],
      center: ['50%', '45%'],
      roseType: 'radius',
      data: topList.value.map(i => ({
        name: i.stallName,
        value: i.popularityScore,
        itemStyle: { color: medalColor(i.rank) }
      })),
      label: { fontSize: 11 }
    }
  ]
}))

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['平均评分', '评价数'], top: 0 },
  grid: { left: 50, right: 50, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: trendList.value.map(i => i.date), boundaryGap: false },
  yAxis: [
    { type: 'value', name: '评分', min: 0, max: 5, splitLine: { lineStyle: { color: '#f0f1f3' } } },
    { type: 'value', name: '评价数', splitLine: { show: false } }
  ],
  series: [
    {
      name: '平均评分',
      type: 'line',
      smooth: true,
      data: trendList.value.map(i => i.avgRating),
      itemStyle: { color: '#ff7a1a' },
      areaStyle: { color: 'rgba(255,122,26,0.12)' }
    },
    {
      name: '评价数',
      type: 'bar',
      yAxisIndex: 1,
      data: trendList.value.map(i => i.reviewCount),
      itemStyle: { color: '#93c5fd' },
      barWidth: 18
    }
  ]
}))

function medalColor(rank) {
  return ['#f59e0b', '#94a3b8', '#d97706'][rank - 1] || '#ff7a1a'
}

function medalType(rank) {
  return ['warning', 'info', 'warning'][rank - 1] || 'primary'
}

function toPercent(score) {
  const max = topList.value.length ? topList.value[0].popularityScore : 1
  return Math.min(100, Math.round((score / (max || 1)) * 100))
}

async function loadOverview() {
  const res = await getOverview()
  overview.value = res.data
}

async function loadRank() {
  const res = await getDishRanking(rankLimit.value)
  rankList.value = res.data
}

async function loadGoodRate() {
  const res = await getStallGoodRate()
  goodRateList.value = res.data
}

async function loadTop() {
  const res = await getStallTop(3)
  topList.value = res.data
}

async function loadTrend() {
  const res = await getRatingTrend(trendDays.value)
  trendList.value = res.data
}

async function loadDishScatter() {
  const res = await getDishScatter()
  dishScatterList.value = res.data
}

async function loadStallScatter() {
  const res = await getStallScatter()
  stallScatterList.value = res.data
}

async function loadAll() {
  await Promise.all([
    loadOverview(), loadRank(), loadGoodRate(), loadTop(), loadTrend(),
    loadDishScatter(), loadStallScatter()
  ])
}

onMounted(loadAll)
// keep-alive 缓存后回访刷新看板数据
onActivated(loadAll)
</script>

<style scoped>
.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  transition: transform .18s ease, box-shadow .18s ease;
  height: 100%;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.metric-icon {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 14px;
  padding: 8px;
}

.metric-icon-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.metric-body {
  min-width: 0;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-1);
  line-height: 1.1;
}

.metric-label {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}

.rank-good {
  border-top: 3px solid #f59e0b;
}

.rank-bad {
  border-top: 3px solid #4b5563;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--border);
}

.rank-row:last-child {
  border-bottom: none;
}

.rank-medal {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank-main {
  flex: 1;
  min-width: 0;
}

.rank-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 6px;
}

.rank-sub {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
}

.rank-score {
  font-size: 18px;
  font-weight: 700;
}

.rank-score.good {
  color: #f59e0b;
}

.rank-score.bad {
  color: #6b7280;
}
</style>