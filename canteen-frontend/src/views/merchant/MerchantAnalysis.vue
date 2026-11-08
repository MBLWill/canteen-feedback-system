<template>
  <div class="page-container analysis-page">
    <!-- 数据总览 -->
    <div class="overview-grid">
      <div class="overview-card" v-for="c in overviewCards" :key="c.label">
        <div class="ov-icon" :style="{ background: c.bg }">{{ c.emoji }}</div>
        <div class="ov-body">
          <div class="ov-value">{{ c.value }}</div>
          <div class="ov-label">{{ c.label }}</div>
        </div>
      </div>
    </div>

    <!-- 两栏瀑布布局 -->
    <div class="analysis-cols">
      <!-- 左栏 -->
      <div class="col-left">
        <!-- 趋势图 -->
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-head">
              <span class="card-title">收入趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadDaily">
                <el-radio-button :value="7">近7天</el-radio-button>
                <el-radio-button :value="30">近一月</el-radio-button>
                <el-radio-button :value="180">近半年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <EChart :option="trendOption" height="300px" />
        </el-card>

        <!-- 最受欢迎食物 -->
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-head"><span class="card-title">最受欢迎的食物</span></div>
          </template>
          <div class="popular-grid">
            <div v-for="d in popularDishes" :key="d.id" class="popular-card" @click="goDish(d)">
              <div class="popular-cover" :style="{ background: coverGradient(d.category) }">
                <img v-if="d.image" :src="d.image" :alt="d.name" loading="lazy">
                <span v-else class="popular-initial">{{ (d.name || '菜').slice(0, 1) }}</span>
                <span v-if="d.avgRating" class="popular-rating">{{ Number(d.avgRating).toFixed(1) }}★</span>
              </div>
              <div class="popular-body">
                <div class="popular-name">{{ d.name }}</div>
                <div class="popular-meta">
                  <span class="popular-price">¥{{ d.price }}</span>
                  <span class="popular-sales">{{ d.salesCount || 0 }} 份售出</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="!popularDishes.length" description="暂无菜品数据" :image-size="60" />
        </el-card>
      </div>

      <!-- 右栏 -->
      <div class="col-right">
        <!-- 畅销商品 -->
        <el-card shadow="never" class="page-card">
          <template #header>
            <div class="card-head"><span class="card-title">畅销商品</span></div>
          </template>
          <div class="best-list">
            <div v-for="(d, i) in bestSellers" :key="d.dishId" class="best-item">
              <span class="best-rank" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
              <div class="best-thumb" :style="{ background: coverGradient('主食') }">
                <img v-if="d.dishImage" :src="d.dishImage" :alt="d.dishName" loading="lazy">
                <span v-else class="best-initial">{{ (d.dishName || '菜').slice(0, 1) }}</span>
              </div>
              <div class="best-info">
                <div class="best-name">{{ d.dishName }}</div>
                <div class="best-amount">¥{{ fmt(d.amount) }}</div>
              </div>
              <span class="best-qty">{{ d.quantity }} 份</span>
            </div>
          </div>
          <el-empty v-if="!bestSellers.length" description="暂无销售数据" :image-size="60" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import EChart from '@/components/EChart.vue'
import { getSalesOverview, getSalesSummary, getDailyIncome } from '@/api/sales'
import { pageDishes } from '@/api/dish'
import { coverGradient } from '@/utils/dishImage'

const router = useRouter()
const daily = ref([])
const monthly = ref([])
const trendDays = ref(7)
const popularDishes = ref([])
const summary = reactive({ dishCount: 0, totalIncome: 0, orderCount: 0, customerCount: 0 })

function fmt(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const overviewCards = computed(() => [
  { label: '菜单总数', value: summary.dishCount ?? 0, emoji: '🍜', bg: 'linear-gradient(135deg,#ffd6a8,#ff9d5c)' },
  { label: '总收入', value: '¥' + fmt(summary.totalIncome), emoji: '💰', bg: 'linear-gradient(135deg,#bfe3ff,#6bb8ff)' },
  { label: '订单数', value: summary.orderCount ?? 0, emoji: '📋', bg: 'linear-gradient(135deg,#cdeecb,#8fd694)' },
  { label: '客户数', value: summary.customerCount ?? 0, emoji: '👥', bg: 'linear-gradient(135deg,#e5d4ff,#b79bf0)' }
])

const bestSellers = computed(() => monthly.value.slice(0, 6))

const trendOption = computed(() => ({
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
    symbolSize: 7,
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(107,184,255,0.4)' },
          { offset: 1, color: 'rgba(107,184,255,0.02)' }
        ]
      }
    }
  }]
}))

function goDish(d) {
  router.push(`/dishes/${d.id}`)
}

async function loadData() {
  const [ov, sm] = await Promise.all([getSalesOverview(), getSalesSummary()])
  monthly.value = ov.data.monthly || []
  Object.assign(summary, sm.data || {})
  loadDaily()
  loadPopular()
}

async function loadDaily() {
  const res = await getDailyIncome(trendDays.value)
  daily.value = res.data || []
}

async function loadPopular() {
  try {
    // 拉取本档口评分最高的菜品作为「最受欢迎」
    const res = await pageDishes({ current: 1, size: 9, sortBy: 'rating' })
    popularDishes.value = res.data.records || []
  } catch {
    popularDishes.value = []
  }
}

onMounted(loadData)
</script>

<style scoped>
.analysis-page { max-width: 1200px; margin: 0 auto; }

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
}

.ov-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}

.ov-value { font-size: 22px; font-weight: 700; color: var(--text-1); line-height: 1.1; }
.ov-label { font-size: 12px; color: var(--text-3); margin-top: 4px; }

.analysis-cols {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
  align-items: start;
}

.col-left, .col-right { display: flex; flex-direction: column; gap: 14px; }

.card-head { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 14px; font-weight: 600; color: var(--text-1); }

/* 最受欢迎食物网格 */
.popular-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.popular-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease;
}

.popular-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }

.popular-cover {
  position: relative;
  height: 100px;
  overflow: hidden;
}

.popular-cover img { width: 100%; height: 100%; object-fit: cover; }

.popular-initial {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  color: #fff;
}

.popular-rating {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.65);
  color: #ffb020;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}

.popular-body { padding: 10px 12px; }
.popular-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.popular-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.popular-price { font-size: 14px; font-weight: 700; color: var(--brand-dark); }
.popular-sales { font-size: 11px; color: var(--text-3); }

/* 畅销商品列表 */
.best-list { display: flex; flex-direction: column; gap: 10px; }

.best-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: var(--bg);
  border-radius: var(--radius-sm);
}

.best-rank {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #cbd5e1;
  flex-shrink: 0;
}
.best-rank.rank-1 { background: #f59e0b; }
.best-rank.rank-2 { background: #94a3b8; }
.best-rank.rank-3 { background: #d97706; }

.best-thumb {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.best-thumb img { width: 100%; height: 100%; object-fit: cover; }
.best-initial { font-size: 18px; font-weight: 700; color: #fff; }

.best-info { flex: 1; min-width: 0; }
.best-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.best-amount { font-size: 12px; color: var(--text-3); margin-top: 2px; }
.best-qty { font-size: 12px; color: var(--brand-dark); font-weight: 600; flex-shrink: 0; }

@media (max-width: 900px) {
  .overview-grid { grid-template-columns: repeat(2, 1fr); }
  .analysis-cols { grid-template-columns: 1fr; }
  .popular-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
