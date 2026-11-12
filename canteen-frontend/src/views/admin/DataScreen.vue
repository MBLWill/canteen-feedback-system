<template>
  <div class="ds" :class="{ 'ds--full': fullscreen, 'ds--loading': loading }">
    <!-- ===== 顶部横幅 ===== -->
    <header class="ds-head">
      <div class="head-left">
        <div class="head-badge">ADMIN · DATA SCREEN</div>
        <h1>食堂运营数据大屏</h1>
        <p class="head-sub">
          数据截至 <b>{{ data.overview?.dataDate || '—' }}</b>
          · <b>{{ data.overview?.stallCount ?? 0 }}</b> 个档口
          / <b>{{ data.overview?.dishCount ?? 0 }}</b> 道在售菜品
          · <b>{{ data.overview?.reviewCount ?? 0 }}</b> 条评价
          · <b>{{ data.overview?.studentCount ?? 0 }}</b> 名学生
        </p>
      </div>

      <div class="head-right">
        <div class="clock">
          <span class="clock-time"><i class="live-dot" />{{ clock.time }}</span>
          <span class="clock-date">{{ clock.date }}</span>
        </div>

        <div class="range-switch">
          <button
            v-for="r in ranges"
            :key="r.value"
            class="range-btn"
            :class="{ active: days === r.value }"
            @click="setDays(r.value)"
          >{{ r.label }}</button>
        </div>

        <button class="pill-btn" :class="{ on: autoRefresh }" :title="autoRefresh ? '关闭自动刷新' : '每 60 秒自动刷新'" @click="toggleAuto">
          <span class="pill-dot" />
          {{ autoRefresh ? '自动刷新' : '手动' }}
        </button>

        <button class="icon-btn" :class="{ spinning: loading }" title="立即刷新" @click="loadAll">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 1 1-2.6-6.4M21 3v6h-6" />
          </svg>
        </button>

        <button
          class="screen-btn"
          :class="{ on: fullscreen }"
          :title="fullscreen ? '退出大屏（Esc）' : '一屏展示全部图表'"
          @click="toggleFullscreen"
        >
          <svg v-if="!fullscreen" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 3H5a2 2 0 0 0-2 2v3M16 3h3a2 2 0 0 1 2 2v3M8 21H5a2 2 0 0 1-2-2v-3M16 21h3a2 2 0 0 0 2-2v-3" />
          </svg>
          <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 8V5a2 2 0 0 1 2-2h3M21 8V5a2 2 0 0 0-2-2h-3M3 16v3a2 2 0 0 0 2 2h3M21 16v3a2 2 0 0 1-2 2h-3" />
          </svg>
          <span>{{ fullscreen ? '退出大屏' : '大屏模式' }}</span>
        </button>
      </div>
    </header>

    <!-- ===== KPI ===== -->
    <div class="kpi-grid">
      <div v-for="(k, i) in kpis" :key="k.key" class="kpi" :style="{ '--accent': k.accent, '--i': i }">
        <div class="kpi-glow" />
        <div class="kpi-top">
          <span class="kpi-icon">{{ k.emoji }}</span>
          <span v-if="k.delta !== null && k.delta !== undefined" class="kpi-delta" :class="k.delta >= 0 ? 'up' : 'down'">
            {{ k.delta >= 0 ? '▲' : '▼' }} {{ Math.abs(k.delta).toFixed(1) }}%
          </span>
          <span v-else class="kpi-tag">{{ k.tag }}</span>
        </div>
        <div class="kpi-value">
          {{ k.value }}<small v-if="k.unit">{{ k.unit }}</small>
        </div>
        <div class="kpi-label">{{ k.label }}</div>
        <div v-if="k.extra" class="kpi-extra">{{ k.extra }}</div>
      </div>
    </div>

    <!-- ===== 经营洞察 ===== -->
    <div class="insight-grid">
      <div v-for="i in insights" :key="i.label" class="insight" :class="'tone-' + i.tone">
        <span class="insight-emoji">{{ i.emoji }}</span>
        <div class="insight-body">
          <div class="insight-label">{{ i.label }}</div>
          <div class="insight-value" :title="i.value">{{ i.value }}</div>
          <div class="insight-hint">{{ i.hint }}</div>
        </div>
      </div>
    </div>

    <!-- ===== 图表区 ===== -->
    <div class="chart-grid">
      <!-- 销量营收趋势 -->
      <section class="card span-8 card--trend">
        <div class="card-head">
          <div>
            <span class="card-title">销量与营收趋势</span>
            <span class="card-sub">近 {{ days }} 天 · 柱=销量(份) / 线=营收(元) · 虚线为日均销量</span>
          </div>
          <div class="legend-inline">
            <i class="dot" style="background:#ff9f45" />销量
            <i class="dot" style="background:#3b82f6; margin-left:12px" />营收
          </div>
        </div>
        <div v-if="data.salesTrend?.length" class="chart-body">
          <EChart :option="trendOption" :height="chartH('286px')" />
        </div>
        <el-empty v-else description="暂无销量数据" :image-size="70" />
      </section>

      <!-- 品类占比 -->
      <section class="card span-4 card--category">
        <div class="card-head">
          <div>
            <span class="card-title">品类销量占比</span>
            <span class="card-sub">点击扇区联动菜品排行</span>
          </div>
          <button v-if="selectedCategory" class="filter-chip" @click="selectedCategory = ''">
            {{ selectedCategory }} ×
          </button>
        </div>
        <div v-if="data.categorySales?.length" class="chart-body">
          <EChart :option="categoryOption" :height="chartH('286px')" @click="onCategoryClick" />
        </div>
        <el-empty v-else description="暂无品类数据" :image-size="70" />
      </section>

      <!-- 档口排行 -->
      <section class="card span-6 card--stall">
        <div class="card-head">
          <div>
            <span class="card-title">档口销量排行</span>
            <span class="card-sub">按购买人数排序 · 点击柱子筛选右侧菜品</span>
          </div>
          <div class="head-tools">
            <div class="mini-seg">
              <button :class="{ active: stallMetric === 'quantity' }" @click="stallMetric = 'quantity'">销量</button>
              <button :class="{ active: stallMetric === 'amount' }" @click="stallMetric = 'amount'">营收</button>
            </div>
            <button v-if="selectedStallId" class="filter-chip" @click="clearStallFilter">
              {{ selectedStallName }} ×
            </button>
          </div>
        </div>
        <div v-if="data.stallRank?.length" class="chart-body">
          <EChart :option="stallRankOption" :height="chartH('300px')" @click="onStallClick" />
        </div>
        <el-empty v-else description="暂无档口数据" :image-size="70" />
      </section>

      <!-- 菜品 TOP -->
      <section class="card span-6 card--dish">
        <div class="card-head">
          <div>
            <span class="card-title">{{ dishTitle }}</span>
            <span class="card-sub">点击柱子查看菜品详情 · 按累计{{ stallMetric === 'amount' ? '营收' : '销量' }}</span>
          </div>
          <div class="mini-seg">
            <button :class="{ active: stallMetric === 'quantity' }" @click="stallMetric = 'quantity'">销量</button>
            <button :class="{ active: stallMetric === 'amount' }" @click="stallMetric = 'amount'">营收</button>
          </div>
        </div>
        <div v-if="filteredDishRank.length" class="chart-body">
          <EChart :option="dishRankOption" :height="chartH('300px')" @click="onDishClick" />
        </div>
        <el-empty v-else description="当前筛选下暂无排行数据" :image-size="70" />
      </section>

      <!-- 档口口碑 -->
      <section class="card span-6 card--praise">
        <div class="card-head">
          <div>
            <span class="card-title">各档口口碑对比</span>
            <span class="card-sub">好评率 = 4-5 星占比 · 低于 60% 标红</span>
          </div>
        </div>
        <div v-if="data.stallRating?.length" class="chart-body">
          <EChart :option="goodRateOption" :height="chartH('300px')" />
        </div>
        <el-empty v-else description="暂无评价数据" :image-size="70" />
      </section>

      <!-- 评分分布 -->
      <section class="card span-6 card--rate">
        <div class="card-head">
          <div>
            <span class="card-title">评分分布</span>
            <span class="card-sub">全校 {{ data.overview?.reviewCount ?? 0 }} 条有效评价 · 平均 {{ Number(data.overview?.avgRating || 0).toFixed(2) }} 分</span>
          </div>
        </div>
        <div v-if="data.ratingDist?.length" class="chart-body">
          <EChart :option="ratingOption" :height="chartH('300px')" />
        </div>
        <el-empty v-else description="暂无评分数据" :image-size="70" />
      </section>

      <!-- 经营象限 -->
      <section class="card span-12 card--quad">
        <div class="card-head">
          <div>
            <span class="card-title">档口经营象限</span>
            <span class="card-sub">横轴=销量，纵轴=好评率，气泡大小=营收 · 右上为「量高口碑好」明星档口，右上分区线为均值</span>
          </div>
        </div>
        <div v-if="quadrantPoints.length" class="chart-body">
          <EChart :option="quadrantOption" :height="chartH('320px')" @click="onStallClick" />
        </div>
        <el-empty v-else description="暂无可对比的档口数据" :image-size="70" />
      </section>
    </div>

    <!-- ===== 大屏模式：次要指标折叠面板（评分分布 / 档口口碑） ===== -->
    <div v-if="fullscreen" class="ds-more">
      <button class="ds-more-btn" :class="{ open: moreOpen }" @click="toggleMore">
        <span class="ds-more-title">更多指标</span>
        <span class="ds-more-hint">评分分布 · 各档口口碑对比</span>
        <span class="ds-more-arrow">{{ moreOpen ? '收起' : '展开' }}</span>
      </button>
    </div>

    <transition name="ds-drawer">
      <div v-if="fullscreen && moreOpen" class="ds-drawer">
        <div class="ds-drawer-head">
          <span class="ds-drawer-title">更多指标</span>
          <button class="ds-drawer-close" title="关闭" @click="moreOpen = false">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>
        <div class="ds-drawer-body">
          <section class="ds-drawer-card">
            <div class="card-head">
              <div>
                <span class="card-title">评分分布</span>
                <span class="card-sub">全校 {{ data.overview?.reviewCount ?? 0 }} 条有效评价 · 平均 {{ Number(data.overview?.avgRating || 0).toFixed(2) }} 分</span>
              </div>
            </div>
            <div v-if="data.ratingDist?.length" class="chart-body">
              <EChart :option="ratingOption" height="100%" />
            </div>
            <el-empty v-else description="暂无评分数据" :image-size="60" />
          </section>

          <section class="ds-drawer-card">
            <div class="card-head">
              <div>
                <span class="card-title">各档口口碑对比</span>
                <span class="card-sub">好评率 = 4-5 星占比 · 低于 60% 标红</span>
              </div>
            </div>
            <div v-if="data.stallRating?.length" class="chart-body">
              <EChart :option="goodRateOption" height="100%" />
            </div>
            <el-empty v-else description="暂无评价数据" :image-size="60" />
          </section>
        </div>
      </div>
    </transition>

    <footer class="ds-foot">
      数据来源：菜品销量汇总 / 评价表 · 趋势为近 {{ days }} 天，排行与评分为累计口径
      <span class="dot-sep">·</span>
      最后刷新 {{ updatedAt || '—' }}
      <span class="dot-sep">·</span>
      仅管理员可见
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import EChart from '@/components/EChart.vue'
import { getDashboard } from '@/api/stats'

const router = useRouter()

const data = ref({
  overview: {},
  salesTrend: [],
  categorySales: [],
  stallRank: [],
  dishRank: [],
  ratingDist: [],
  stallRating: []
})

const days = ref(30)
const loading = ref(false)
const fullscreen = ref(false)
// 大屏下「更多指标」折叠面板（评分分布 / 档口口碑），默认收起
const moreOpen = ref(false)
const autoRefresh = ref(false)
const updatedAt = ref('')
const selectedStallId = ref(null)
const selectedStallName = ref('')
const selectedCategory = ref('')
/** 排行指标：销量 / 营收（档口榜与菜品榜共用，保持口径一致） */
const stallMetric = ref('quantity')

const ranges = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 }
]

const PALETTE = ['#ff7a1a', '#3b82f6', '#22c55e', '#f59e0b', '#a855f7', '#ec4899', '#14b8a6', '#f43f5e']

/** #rrggbb → rgba()，用于气泡光晕等需要透明度的场景 */
function rgba(hex, a) {
  const n = parseInt(String(hex).replace('#', ''), 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${a})`
}

/** 坐标轴上限取整到「半档」，避免出现 4407.3 这类零碎刻度 */
function niceMax(v) {
  const n = Math.max(1, Number(v) || 1)
  const mag = Math.pow(10, Math.floor(Math.log10(n)))
  const step = mag / 2
  return Math.ceil((n * 1.12) / step) * step
}

/* ---------- 实时时钟 ---------- */
const clock = ref({ time: '', date: '' })
let clockTimer = null
function tickClock() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  clock.value = {
    time: `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`,
    date: `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${week}`
  }
}

/* ---------- 格式化 ---------- */
function fmtMoney(v) {
  const n = Number(v || 0)
  return n >= 10000 ? (n / 10000).toFixed(2) : n.toFixed(0)
}
function moneyUnit(v) {
  return Number(v || 0) >= 10000 ? ' 万元' : ' 元'
}
function fmtQty(v) {
  const n = Number(v || 0)
  return n >= 10000 ? (n / 10000).toFixed(2) + ' 万' : Math.round(n).toLocaleString()
}
function fmtValue(v) {
  return Number(v || 0) >= 10000 ? (Number(v) / 10000).toFixed(2) + ' 万' : Math.round(Number(v || 0)).toLocaleString()
}

/* ---------- KPI 数字滚动 ---------- */
const nums = reactive({
  totalSalesQty: 0,
  totalSalesAmount: 0,
  avgDailyQty: 0,
  latestDayQty: 0,
  avgRating: 0,
  goodRate: 0
})
let rafId = null
function tweenNumbers() {
  const o = data.value.overview || {}
  const target = {
    totalSalesQty: Number(o.totalSalesQty || 0),
    totalSalesAmount: Number(o.totalSalesAmount || 0),
    avgDailyQty: Number(o.avgDailyQty || 0),
    latestDayQty: Number(o.latestDayQty || 0),
    avgRating: Number(o.avgRating || 0),
    goodRate: Number(o.goodRate || 0)
  }
  const start = { ...nums }
  if (rafId) cancelAnimationFrame(rafId)
  const t0 = performance.now()
  const dur = 700
  const step = now => {
    const p = Math.min(1, (now - t0) / dur)
    const e = 1 - Math.pow(1 - p, 3)
    Object.keys(target).forEach(k => { nums[k] = start[k] + (target[k] - start[k]) * e })
    if (p < 1) rafId = requestAnimationFrame(step)
  }
  rafId = requestAnimationFrame(step)
}

/* ---------- KPI ---------- */
const kpis = computed(() => {
  const o = data.value.overview || {}
  const avg = Number(o.avgDailyQty || 0)
  const latest = Number(nums.latestDayQty || 0)
  const delta = avg > 0 ? ((latest - avg) / avg) * 100 : null
  return [
    { key: 'qty', label: '累计销量', value: fmtQty(nums.totalSalesQty), unit: ' 份', emoji: '🍚', tag: '总', accent: '#ff7a1a' },
    { key: 'amt', label: '累计营收', value: fmtMoney(nums.totalSalesAmount), unit: moneyUnit(o.totalSalesAmount), emoji: '💰', tag: '总', accent: '#f59e0b' },
    { key: 'avg', label: '日均销量', value: fmtQty(nums.avgDailyQty), unit: ' 份', emoji: '📅', tag: '均', accent: '#3b82f6' },
    { key: 'latest', label: '当日销量', value: fmtQty(nums.latestDayQty), unit: ' 份', emoji: '⚡', accent: '#22c55e', delta, extra: `当日营收 ¥${Number(o.latestDayAmount || 0).toLocaleString()}` },
    { key: 'rating', label: '平均评分', value: Number(nums.avgRating || 0).toFixed(2), unit: ' 分', emoji: '⭐', tag: '评', accent: '#a855f7', extra: `${o.reviewCount ?? 0} 条评价` },
    { key: 'good', label: '好评率', value: Number(nums.goodRate || 0).toFixed(1), unit: ' %', emoji: '👍', tag: '率', accent: '#ec4899', extra: '4-5 星占比' }
  ]
})

/* ---------- 经营洞察 ---------- */
const insights = computed(() => {
  const out = []
  const stalls = data.value.stallRank || []

  // 销量冠军
  if (stalls.length) {
    const top = stalls[0]
    const second = stalls[1]
    const lead = second && Number(second.quantity) ? ((Number(top.quantity) - Number(second.quantity)) / Number(second.quantity)) * 100 : null
    out.push({
      emoji: '🏆',
      label: '销量冠军',
      value: top.stallName,
      hint: `${fmtValue(top.quantity)} 份` + (lead === null ? '' : ` · 领先第二 ${lead.toFixed(1)}%`),
      tone: 'gold'
    })
  }

  // 近期环比（后半段 vs 前半段），配色遵循「涨红跌绿」
  const trend = data.value.salesTrend || []
  if (trend.length >= 4) {
    const half = Math.floor(trend.length / 2)
    const prev = trend.slice(0, half).reduce((s, i) => s + Number(i.quantity || 0), 0)
    const cur = trend.slice(half).reduce((s, i) => s + Number(i.quantity || 0), 0)
    const delta = prev ? ((cur - prev) / prev) * 100 : 0
    out.push({
      emoji: delta >= 0 ? '📈' : '📉',
      label: '近期环比',
      value: `${delta >= 0 ? '+' : ''}${delta.toFixed(1)}%`,
      hint: `后 ${half} 天 vs 前 ${trend.length - half} 天销量`,
      tone: delta >= 0 ? 'red' : 'green'
    })
  }

  // 口碑最佳 / 待改进
  const rated = (data.value.stallRating || []).filter(i => Number(i.reviewCount) >= 3)
  if (rated.length) {
    const best = [...rated].sort((a, b) => Number(b.goodRate) - Number(a.goodRate))[0]
    out.push({
      emoji: '🌟',
      label: '口碑最佳',
      value: best.stallName,
      hint: `好评率 ${Number(best.goodRate || 0).toFixed(1)}% · ${best.reviewCount} 条评价`,
      tone: 'blue'
    })
    const worst = [...rated].sort((a, b) => Number(a.goodRate) - Number(b.goodRate))[0]
    if (worst && worst.stallId !== best.stallId) {
      out.push({
        emoji: '⚠️',
        label: '待改进档口',
        value: worst.stallName,
        hint: `好评率 ${Number(worst.goodRate || 0).toFixed(1)}% · 建议跟进`,
        tone: 'red'
      })
    }
  }

  // 品类之王
  const cats = data.value.categorySales || []
  if (cats.length && out.length < 5) {
    const topCat = cats[0]
    out.push({
      emoji: '🍜',
      label: '品类之王',
      value: topCat.category || '其他',
      hint: `${fmtValue(topCat.quantity)} 份 · 营收 ¥${Number(topCat.amount || 0).toLocaleString()}`,
      tone: 'orange'
    })
  }

  return out.slice(0, 4)
})

/* ---------- 销量趋势 ---------- */
const trendOption = computed(() => {
  const list = data.value.salesTrend || []
  const avgQty = list.length ? list.reduce((s, i) => s + Number(i.quantity || 0), 0) / list.length : 0
  return {
    animationDuration: 900,
    animationDurationUpdate: 480,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross', crossStyle: { color: '#c9cfd8' } },
      formatter: params => {
        if (!params?.length) return ''
        const idx = params[0].dataIndex
        const raw = list[idx] || {}
        const lines = [`<b>${raw.date || params[0].axisValue}</b>`]
        params.forEach(p => {
          const val = p.seriesName === '营收' ? `¥${Number(p.value || 0).toLocaleString()}` : `${Number(p.value || 0).toLocaleString()} 份`
          lines.push(`${p.marker}${p.seriesName}：${val}`)
        })
        return lines.join('<br/>')
      }
    },
    grid: { left: 10, right: 32, top: 40, bottom: 14, containLabel: true },
    xAxis: {
      type: 'category',
      data: list.map(i => (i.date || '').slice(5)),
      boundaryGap: true,
      axisLine: { lineStyle: { color: '#e2e6ec' } },
      axisTick: { show: false },
      axisLabel: { color: '#98a1ad', fontSize: 12, interval: Math.max(0, Math.floor(list.length / 10)) }
    },
    yAxis: [
      {
        type: 'value', name: '销量', nameTextStyle: { color: '#98a1ad', fontSize: 12 },
        splitLine: { lineStyle: { color: '#f1f4f8' } },
        axisLabel: { color: '#98a1ad', fontSize: 12 }
      },
      {
        type: 'value', name: '营收', nameTextStyle: { color: '#98a1ad', fontSize: 12 },
        splitLine: { show: false },
        axisLabel: { color: '#98a1ad', fontSize: 12, formatter: v => (v >= 10000 ? (v / 10000).toFixed(1) + 'w' : v) }
      }
    ],
    series: [
      {
        name: '销量',
        type: 'bar',
        barMaxWidth: 24,
        animationDelay: idx => idx * 24,
        data: list.map(i => i.quantity),
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: '#ffb066' }, { offset: 1, color: '#ff7a1a' }]
          }
        },
        emphasis: { itemStyle: { color: '#e35f00' } },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { type: 'dashed', color: '#c9cfd8', width: 1 },
          label: {
            formatter: `日均 ${Math.round(avgQty)}`,
            position: 'insideEndTop',
            color: '#98a1ad',
            fontSize: 10
          },
          data: [{ yAxis: avgQty }]
        }
      },
      {
        name: '营收',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: list.map(i => i.amount),
        lineStyle: { width: 2.4, color: '#3b82f6' },
        itemStyle: { color: '#3b82f6' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.22)' }, { offset: 1, color: 'rgba(59,130,246,0.01)' }]
          }
        }
      }
    ]
  }
})

/* ---------- 品类占比 ---------- */
const categoryOption = computed(() => {
  const list = data.value.categorySales || []
  const total = list.reduce((s, i) => s + Number(i.quantity || 0), 0)
  // 大屏下卡片更宽，环图靠左、图例竖排在右侧，避免中间一大片空白
  const wide = fullscreen.value
  const centerX = wide ? '27%' : '50%'
  const centerY = wide ? '50%' : '44%'
  const pctOf = name => {
    const it = list.find(i => (i.category || '其他') === name)
    return total ? ((Number(it?.quantity || 0) / total) * 100).toFixed(1) : '0'
  }
  return {
    animationDuration: 900,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'item',
      formatter: p => {
        const pct = total ? ((p.value / total) * 100).toFixed(1) : 0
        return `<b>${p.name}</b><br/>销量：${Number(p.value).toLocaleString()} 份<br/>占比：${pct}%<br/>营收：¥${Number(p.data.amount || 0).toLocaleString()}`
      }
    },
    legend: wide
      ? {
          orient: 'vertical',
          right: 14,
          top: 'middle',
          itemWidth: 10,
          itemHeight: 10,
          itemGap: 13,
          textStyle: { color: '#5b6472', fontSize: 12 },
          formatter: name => `${name}   ${pctOf(name)}%`
        }
      : {
          bottom: 0, itemWidth: 9, itemHeight: 9, itemGap: 12,
          textStyle: { color: '#5b6472', fontSize: 11 }
        },
    series: [
      {
        type: 'pie',
        radius: wide ? ['52%', '88%'] : ['46%', '68%'],
        center: [centerX, centerY],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
        label: { show: !wide, formatter: '{d}%', fontSize: 11, color: '#5b6472' },
        labelLine: { show: !wide, length: 8, length2: 8, lineStyle: { color: '#d5dbe3' } },
        emphasis: {
          scaleSize: 8,
          label: { show: true, fontSize: 13, fontWeight: 'bold', formatter: '{b}\n{d}%' }
        },
        selectedMode: 'single',
        data: list.map((i, idx) => ({
          name: i.category || '其他',
          value: Number(i.quantity || 0),
          amount: i.amount,
          selected: selectedCategory.value === (i.category || '其他'),
          itemStyle: { color: PALETTE[idx % PALETTE.length] }
        }))
      }
    ],
    graphic: [
      { type: 'text', z: 10, left: centerX, top: wide ? '42%' : '38%', style: { text: '总销量', fill: '#98a1ad', fontSize: 11, textAlign: 'center' } },
      {
        type: 'text', z: 10, left: centerX, top: wide ? '49%' : '45%',
        style: {
          text: total >= 10000 ? (total / 10000).toFixed(1) + '万' : String(total),
          fill: '#1a1d24', fontSize: 20, fontWeight: 'bold', textAlign: 'center'
        }
      }
    ]
  }
})

/* ---------- 档口排行 ---------- */
const stallRankOption = computed(() => {
  // 大屏模式一屏信息更多，榜单收敛到前 8，条形更舒展
  const top = (data.value.stallRank || []).slice(0, fullscreen.value ? 8 : 10)
  const list = [...top].reverse()
  const isAmount = stallMetric.value === 'amount'
  const max = Math.max(1, ...list.map(i => Number(i[stallMetric.value] || 0)))
  return {
    animationDuration: 800,
    animationDurationUpdate: 480,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const p = params[0]
        const raw = list.find(i => i.stallName === p.name) || {}
        return `<b>${raw.stallName || p.name}</b>（${raw.areaLabel || ''} · ${raw.windowNo || ''} 号）<br/>` +
          `销量：${Number(raw.quantity || 0).toLocaleString()} 份<br/>` +
          `营收：¥${Number(raw.amount || 0).toLocaleString()}<br/>` +
          `评分：${Number(raw.avgRating || 0).toFixed(2)} 分`
      }
    },
    grid: { left: 6, right: 56, top: 18, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      max: niceMax(max),
      splitLine: { lineStyle: { color: '#f1f4f8' } },
      axisLabel: { color: '#98a1ad', fontSize: 12, formatter: v => (v >= 10000 ? (v / 10000).toFixed(1) + 'w' : v) }
    },
    yAxis: {
      type: 'category',
      data: list.map(i => i.stallName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#5b6472', fontSize: 12, interval: 0, formatter: v => (v.length > 6 ? v.slice(0, 6) + '…' : v) }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 22,
        animationDelay: idx => idx * 40,
        data: list.map(i => ({
          value: i[stallMetric.value],
          stallId: i.stallId,
          stallName: i.stallName,
          itemStyle: {
            borderRadius: [0, 7, 7, 0],
            color: selectedStallId.value && i.stallId === selectedStallId.value
              ? '#e35f00'
              : {
                  type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
                  colorStops: [{ offset: 0, color: isAmount ? '#9ec8ff' : '#ffcfa3' }, { offset: 1, color: isAmount ? '#2563eb' : '#ff7a1a' }]
                }
          }
        })),
        label: {
          show: true,
          position: 'right',
          formatter: p => (isAmount ? '¥' + fmtValue(p.value) : fmtValue(p.value)),
          color: '#8a93a0',
          fontSize: 11
        },
        emphasis: { itemStyle: { color: '#e35f00' } }
      }
    ]
  }
})

/* ---------- 菜品 TOP ---------- */
const filteredDishRank = computed(() => {
  let list = data.value.dishRank || []
  if (selectedStallId.value) list = list.filter(i => i.stallId === selectedStallId.value)
  if (selectedCategory.value) list = list.filter(i => (i.category || '其他') === selectedCategory.value)
  const filtered = !!(selectedStallId.value || selectedCategory.value)
  return filtered ? list : list.slice(0, fullscreen.value ? 8 : 10)
})

const dishTitle = computed(() => {
  if (selectedStallName.value) return `${selectedStallName.value} · 热销菜品`
  if (selectedCategory.value) return `${selectedCategory.value} · 热销菜品`
  return `菜品销量 TOP${fullscreen.value ? 8 : 10}`
})

const dishRankOption = computed(() => {
  const list = [...filteredDishRank.value].reverse()
  const isAmount = stallMetric.value === 'amount'
  const max = Math.max(1, ...list.map(i => Number(i[stallMetric.value] || 0)))
  return {
    animationDuration: 800,
    animationDurationUpdate: 480,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const p = params[0]
        const raw = list.find(i => i.dishName === p.name) || {}
        return `<b>${raw.dishName || p.name}</b><br/>` +
          `档口：${raw.stallName || '-'} · ${raw.category || '其他'}<br/>` +
          `销量：${Number(raw.quantity || 0).toLocaleString()} 份<br/>` +
          `营收：¥${Number(raw.amount || 0).toLocaleString()}<br/>` +
          `单价：¥${raw.price || 0}`
      }
    },
    grid: { left: 6, right: 54, top: 18, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      max: niceMax(max),
      splitLine: { lineStyle: { color: '#f1f4f8' } },
      axisLabel: { color: '#98a1ad', fontSize: 12, formatter: v => (v >= 10000 ? (v / 10000).toFixed(1) + 'w' : v) }
    },
    yAxis: {
      type: 'category',
      data: list.map(i => i.dishName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#5b6472', fontSize: 12, interval: 0, formatter: v => (v.length > 7 ? v.slice(0, 7) + '…' : v) }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 20,
        animationDelay: idx => idx * 40,
        data: list.map((i, idx) => ({
          value: i[stallMetric.value],
          dishId: i.dishId,
          stallId: i.stallId,
          itemStyle: {
            borderRadius: [0, 7, 7, 0],
            color: {
              type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
              colorStops: [
                { offset: 0, color: PALETTE[idx % PALETTE.length] + '55' },
                { offset: 1, color: PALETTE[idx % PALETTE.length] }
              ]
            }
          }
        })),
        label: {
          show: true,
          position: 'right',
          formatter: p => (isAmount ? '¥' + fmtValue(p.value) : fmtValue(p.value)),
          color: '#8a93a0',
          fontSize: 11
        }
      }
    ]
  }
})

/* ---------- 档口好评率 ---------- */
const goodRateOption = computed(() => {
  const list = [...(data.value.stallRating || [])].sort((a, b) => a.goodRate - b.goodRate)
  return {
    animationDuration: 800,
    animationDurationUpdate: 480,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const p = params[0]
        const raw = list.find(i => i.stallName === p.name) || {}
        return `<b>${raw.stallName || p.name}</b>（${raw.windowNo || ''} 号）<br/>` +
          `好评率：${Number(p.value || 0).toFixed(1)}%<br/>` +
          `平均评分：${Number(raw.avgRating || 0).toFixed(2)} 分<br/>` +
          `评价数：${raw.reviewCount || 0} 条`
      }
    },
    grid: { left: 6, right: 50, top: 18, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value', max: 100,
      splitLine: { lineStyle: { color: '#f1f4f8' } },
      axisLabel: { color: '#98a1ad', fontSize: 12, formatter: '{value}%' }
    },
    yAxis: {
      type: 'category',
      data: list.map(i => i.stallName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#5b6472', fontSize: 12, interval: 0, formatter: v => (v.length > 6 ? v.slice(0, 6) + '…' : v) }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 16,
        animationDelay: idx => idx * 40,
        data: list.map(i => ({
          value: i.goodRate,
          itemStyle: {
            borderRadius: [0, 7, 7, 0],
            color: i.goodRate >= 80 ? '#22c55e' : i.goodRate >= 60 ? '#f59e0b' : '#ef4444'
          }
        })),
        label: {
          show: true,
          position: 'right',
          formatter: p => Number(p.value || 0).toFixed(1) + '%',
          color: '#8a93a0',
          fontSize: 11
        }
      }
    ]
  }
})

/* ---------- 评分分布 ---------- */
const ratingOption = computed(() => {
  const list = [...(data.value.ratingDist || [])].sort((a, b) => a.star - b.star)
  const total = list.reduce((s, i) => s + Number(i.count || 0), 0)
  const COLORS = ['#ef4444', '#f97316', '#f59e0b', '#84cc16', '#22c55e']
  return {
    animationDuration: 800,
    animationDurationUpdate: 480,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const p = params[0]
        const pct = total ? ((p.value / total) * 100).toFixed(1) : 0
        return `<b>${p.name}</b><br/>数量：${Number(p.value || 0).toLocaleString()} 条<br/>占比：${pct}%`
      }
    },
    grid: { left: 6, right: 12, top: 34, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: list.map(i => `${i.star} 星`),
      axisLine: { lineStyle: { color: '#e2e6ec' } },
      axisTick: { show: false },
      axisLabel: { color: '#5b6472', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f4f8' } },
      axisLabel: { color: '#98a1ad', fontSize: 12 }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 42,
        animationDelay: idx => idx * 60,
        data: list.map((i, idx) => ({
          value: i.count,
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [{ offset: 0, color: COLORS[idx] }, { offset: 1, color: COLORS[idx] + '77' }]
            }
          }
        })),
        label: {
          show: true,
          position: 'top',
          color: '#5b6472',
          fontSize: 11,
          formatter: p => {
            const pct = total ? ((p.value / total) * 100).toFixed(0) : 0
            return `${p.value} 条 · ${pct}%`
          }
        }
      }
    ]
  }
})

/* ---------- 经营象限 ---------- */
const quadrantPoints = computed(() => {
  const rateMap = new Map((data.value.stallRating || []).map(i => [String(i.stallId), i]))
  return (data.value.stallRank || [])
    .map(s => {
      const r = rateMap.get(String(s.stallId))
      if (!r || !Number(r.reviewCount)) return null
      return {
        stallId: s.stallId,
        stallName: s.stallName,
        windowNo: s.windowNo,
        quantity: Number(s.quantity || 0),
        amount: Number(s.amount || 0),
        goodRate: Number(r.goodRate || 0),
        avgRating: Number(r.avgRating || 0),
        reviewCount: Number(r.reviewCount || 0)
      }
    })
    .filter(Boolean)
})

const quadrantOption = computed(() => {
  const pts = quadrantPoints.value
  const avgQty = pts.length ? pts.reduce((s, i) => s + i.quantity, 0) / pts.length : 0
  const avgRate = pts.length ? pts.reduce((s, i) => s + i.goodRate, 0) / pts.length : 0
  const rawMaxQty = Math.max(1, ...pts.map(i => i.quantity))
  const rawMinQty = Math.min(...pts.map(i => i.quantity))
  const qtyStep = rawMaxQty > 2000 ? 250 : 100
  // 两端留白收紧，点位更铺开（象限判断依赖均值线，不必从 0 起）
  const minQty = Math.max(0, Math.floor((rawMinQty * 0.78) / qtyStep) * qtyStep)
  const maxQty = Math.ceil((rawMaxQty * 1.06) / qtyStep) * qtyStep
  const maxAmount = Math.max(1, ...pts.map(i => i.amount))
  const colorOf = p => {
    if (p.quantity >= avgQty && p.goodRate >= avgRate) return '#22c55e'
    if (p.quantity >= avgQty) return '#f59e0b'
    if (p.goodRate >= avgRate) return '#3b82f6'
    return '#94a3b8'
  }
  const area = (x0, x1, y0, y1, color, text, pos) => ([
    {
      xAxis: x0,
      yAxis: y0,
      itemStyle: { color },
      label: {
        show: !!text,
        position: pos,
        formatter: text,
        color: '#c3cad4',
        fontSize: 10,
        fontWeight: 600,
        padding: [6, 8]
      }
    },
    { xAxis: x1, yAxis: y1 }
  ])
  return {
    animationDuration: 900,
    animationDurationUpdate: 500,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'item',
      formatter: p => {
        const d = p.data.raw
        return `<b>${d.stallName}</b>（${d.windowNo || ''} 号）<br/>` +
          `销量：${d.quantity.toLocaleString()} 份<br/>` +
          `营收：¥${d.amount.toLocaleString()}<br/>` +
          `好评率：${d.goodRate.toFixed(1)}%（${d.reviewCount} 条）<br/>` +
          `平均分：${d.avgRating.toFixed(2)}`
      }
    },
    grid: { left: 8, right: 34, top: 30, bottom: 12, containLabel: true },
    xAxis: {
      type: 'value',
      name: '销量(份)',
      nameLocation: 'middle',
      nameGap: 26,
      nameTextStyle: { color: '#98a1ad', fontSize: 12 },
      min: minQty,
      max: maxQty,
      splitLine: { lineStyle: { color: '#f1f4f8' } },
      axisLabel: { color: '#98a1ad', fontSize: 12, formatter: v => (v >= 10000 ? (v / 10000).toFixed(1) + 'w' : v) }
    },
    yAxis: {
      type: 'value',
      name: '好评率(%)',
      nameTextStyle: { color: '#98a1ad', fontSize: 12 },
      min: v => Math.max(0, Math.floor((v.min - 4) / 10) * 10),
      max: v => Math.min(100, Math.ceil((v.max + 4) / 10) * 10),
      splitNumber: 4,
      splitLine: { lineStyle: { color: '#f1f4f8' } },
      axisLabel: { color: '#98a1ad', fontSize: 12, formatter: '{value}%' }
    },
    series: [
      // 底层：扩散涟漪（只做视觉，不参与交互）
      {
        type: 'effectScatter',
        silent: true,
        z: 1,
        symbolSize: v => 10 + Math.sqrt(v[2] / maxAmount) * 16,
        rippleEffect: { period: 4.2, scale: 2.9, brushType: 'stroke', number: 2 },
        data: pts.map(p => ({
          value: [p.quantity, p.goodRate, p.amount],
          itemStyle: { color: colorOf(p), opacity: 0.55 }
        }))
      },
      // 上层：实心气泡
      {
        type: 'scatter',
        z: 3,
        symbolSize: v => 9 + Math.sqrt(v[2] / maxAmount) * 14,
        data: pts.map(p => ({
          value: [p.quantity, p.goodRate, p.amount],
          raw: p,
          stallId: p.stallId,
          itemStyle: {
            color: colorOf(p),
            opacity: 0.9,
            borderColor: '#fff',
            borderWidth: 1.5,
            shadowBlur: 12,
            shadowColor: rgba(colorOf(p), 0.4)
          }
        })),
        label: {
          show: true,
          position: 'top',
          distance: 6,
          formatter: p => (!fullscreen.value || p.dataIndex < 3) ? p.data.raw.stallName : '',
          color: '#5b6472',
          fontSize: fullscreen.value ? 10 : 11,
          textBorderColor: 'rgba(255,255,255,0.92)',
          textBorderWidth: 2
        },
        // 点位靠近时标签自动上下错开，避免文字叠在一起
        labelLayout: { moveOverlap: 'shiftY', hideOverlap: fullscreen.value },
        emphasis: {
          scale: 1.15,
          itemStyle: { opacity: 1, borderWidth: 1.6, shadowBlur: 18 }
        },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: '#cfd6df', type: 'dashed', width: 1 },
          label: { show: false },
          data: [{ xAxis: avgQty }, { yAxis: avgRate }]
        },
        markArea: {
          silent: true,
          z: 0,
          data: [
            area(avgQty, maxQty, avgRate, 100, 'rgba(34,197,94,0.055)', (fullscreen.value ? '' : '量高口碑好'), 'insideTopRight'),
            area(0, avgQty, avgRate, 100, 'rgba(59,130,246,0.045)', (fullscreen.value ? '' : '口碑好'), 'insideTopLeft'),
            area(avgQty, maxQty, 0, avgRate, 'rgba(245,158,11,0.05)', (fullscreen.value ? '' : '量高待提升'), 'insideBottomRight'),
            area(0, avgQty, 0, avgRate, 'rgba(148,163,184,0.055)', (fullscreen.value ? '' : '待改进'), 'insideBottomLeft')
          ]
        }
      }
    ]
  }
})

/* ---------- 交互 ---------- */
function setDays(v) {
  if (days.value === v) return
  days.value = v
  loadAll()
}

function clearStallFilter() {
  selectedStallId.value = null
  selectedStallName.value = ''
}

/** 点击档口柱：联动筛选菜品排行，再次点击取消 */
function onStallClick(params) {
  const sid = params?.data?.stallId
  if (!sid) return
  if (selectedStallId.value === sid) {
    clearStallFilter()
    return
  }
  selectedStallId.value = sid
  selectedStallName.value = params.data.stallName || params.data?.raw?.stallName || ''
}

/** 点击品类扇区：按品类筛选菜品排行 */
function onCategoryClick(params) {
  const name = params?.name
  if (!name) return
  selectedCategory.value = selectedCategory.value === name ? '' : name
}

/** 点击菜品柱：跳转菜品详情 */
function onDishClick(params) {
  const id = params?.data?.dishId
  if (!id) return
  router.push(`/dishes/${id}`)
}

/* ---------- 自动刷新 ---------- */
let autoTimer = null
function toggleAuto() {
  autoRefresh.value = !autoRefresh.value
  if (autoTimer) {
    clearInterval(autoTimer)
    autoTimer = null
  }
  if (autoRefresh.value) {
    autoTimer = setInterval(() => { loadAll(true) }, 60000)
  }
}

/* ---------- 大屏模式 ---------- */
/** 大屏下一屏铺满：图表高度交给 flex 容器，普通模式下用各自的设计高度 */
function chartH(base) {
  return fullscreen.value ? '100%' : base
}

async function toggleFullscreen() {
  const next = !fullscreen.value
  fullscreen.value = next
  try {
    if (next) {
      if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
        await document.documentElement.requestFullscreen()
      }
    } else if (document.fullscreenElement && document.exitFullscreen) {
      await document.exitFullscreen()
    }
  } catch {
    // 预览面板等受限于 iframe 时浏览器会拒绝，遮罩层已经铺满视口，忽略即可
  }
}

/** 用户用系统方式退出全屏时同步内部状态 */
function onFsChange() {
  if (!document.fullscreenElement && fullscreen.value) fullscreen.value = false
}

function onKeydown(e) {
  if (e.key === 'Escape' && fullscreen.value) {
    fullscreen.value = false
    if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(() => {})
  }
}
function toggleMore() {
  moreOpen.value = !moreOpen.value
}

watch(fullscreen, v => {
  document.body.style.overflow = v ? 'hidden' : ''
  if (!v) moreOpen.value = false
})

/* ---------- 数据加载 ---------- */
async function loadAll(silent = false) {
  if (loading.value) return
  if (!silent) loading.value = true
  try {
    const res = await getDashboard(days.value)
    data.value = res.data || data.value
    tweenNumbers()
    const p = n => String(n).padStart(2, '0')
    const d = new Date()
    updatedAt.value = `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
    // 筛选的档口/品类可能已不在榜单内，自动清理
    if (selectedStallId.value && !(data.value.stallRank || []).some(s => s.stallId === selectedStallId.value)) {
      clearStallFilter()
    }
    if (selectedCategory.value && !(data.value.categorySales || []).some(c => (c.category || '其他') === selectedCategory.value)) {
      selectedCategory.value = ''
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  tickClock()
  clockTimer = setInterval(tickClock, 1000)
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('fullscreenchange', onFsChange)
  await loadAll()
})

onActivated(() => {
  loadAll(true)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (autoTimer) clearInterval(autoTimer)
  if (rafId) cancelAnimationFrame(rafId)
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('fullscreenchange', onFsChange)
  if (document.fullscreenElement && document.exitFullscreen) document.exitFullscreen().catch(() => {})
  document.body.style.overflow = ''
})
</script>

<style scoped>
.ds {
  position: relative;
  isolation: isolate;
  padding: 16px 18px 22px;
  min-height: 100%;
  background: var(--bg);
}

/*
 * 背景光晕放在伪元素上：路由 <transition> 会把组件根元素的 animation 当成过渡动画来等待，
 * 根元素挂无限动画会导致离开过渡永远结束不了、旧页面残留（其它菜单点开像是没反应）。
 */
.ds::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    radial-gradient(1100px 380px at 8% -10%, rgba(255, 122, 26, .10), transparent 62%),
    radial-gradient(900px 360px at 100% 0%, rgba(59, 130, 246, .09), transparent 60%);
  background-size: 160% 160%, 150% 150%;
  animation: bgDrift 26s ease-in-out infinite alternate;
}

/* 背景光晕缓慢漂移，让静态看板有呼吸感 */
@keyframes bgDrift {
  from { background-position: 0% 0%, 100% 0%, 0 0; }
  to { background-position: 45% 35%, 55% 25%, 0 0; }
}

/* 全屏大屏模式：覆盖整个视口 */
/* 大屏模式：铺满视口，一屏装下全部图表（不滚动） */
.ds--full {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
  padding: 12px 16px 12px;
  animation: fsIn .3s cubic-bezier(.22, .8, .3, 1) both;
}

/* 顶部收成一行标题，减少信息层级 */
.ds--full .ds-head {
  padding: 10px 20px;
  margin-bottom: 0;
}

.ds--full .head-left {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.ds--full .head-badge { display: none; }
.ds--full .ds-head h1 { font-size: 19px; letter-spacing: .4px; }
.ds--full .head-sub { margin: 0; font-size: 11px; color: rgba(255, 255, 255, .6); }
.ds--full .head-sub b { color: #ffd7b0; }
.ds--full .clock { padding-right: 12px; }
.ds--full .clock-time { font-size: 17px; }
.ds--full .clock-date { font-size: 10px; }
.ds--full .range-btn { padding: 6px 12px; }

/* 六项指标合并成一条，视觉上从 6 张卡变成 1 个模块 */
.ds--full .kpi-grid {
  grid-template-columns: repeat(6, 1fr);
  gap: 0;
  padding: 9px 6px;
  margin-bottom: 0;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-xs);
}

.ds--full .kpi {
  padding: 0 20px;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  overflow: visible;
  animation: none;
}

.ds--full .kpi + .kpi { border-left: 1px solid var(--border); }
.ds--full .kpi::before,
.ds--full .kpi-glow { display: none; }
.ds--full .kpi:hover { transform: none; box-shadow: none; }
.ds--full .kpi-top { justify-content: flex-start; gap: 8px; margin-bottom: 4px; min-height: 16px; }
.ds--full .kpi-icon { font-size: 15px; }
.ds--full .kpi-tag { display: none; }
.ds--full .kpi-value { font-size: 22px; }
.ds--full .kpi-label { margin-top: 3px; font-size: 11.5px; }
.ds--full .kpi-extra { display: none; }

/* 洞察条与页脚在大屏下让位给图表 */
.ds--full .insight-grid,
.ds--full .ds-foot { display: none; }

/* ---------- 大屏：核心 4 行布局 ---------- */
.ds--full .chart-grid {
  flex: 1;
  min-height: 0;
  grid-template-columns: repeat(12, 1fr);
  /* 趋势为主视觉，第三行要放下 8 条横条，故给得最高 */
  grid-template-rows: 1.08fr 1.28fr 1fr;
  gap: 16px;
}

/* 第 2 行：销量营收趋势通栏（视觉 C 位） */
.ds--full .card--trend { grid-area: 1 / 1 / 2 / 13; }
/* 第 3 行：档口销量排行 | 菜品 TOP8 */
.ds--full .card--stall { grid-area: 2 / 1 / 3 / 7; }
.ds--full .card--dish { grid-area: 2 / 7 / 3 / 13; }
/* 第 4 行：档口经营象限 | 品类占比 */
.ds--full .card--quad { grid-area: 3 / 1 / 4 / 7; }
.ds--full .card--category { grid-area: 3 / 7 / 4 / 13; }

.ds--full .card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 16px 18px 12px;
  border: none;
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, .04), 0 12px 30px rgba(16, 24, 40, .07);
  animation: none;
}

.ds--full .card:hover {
  transform: none;
  box-shadow: 0 1px 2px rgba(16, 24, 40, .04), 0 12px 30px rgba(16, 24, 40, .07);
}

/* 次要指标移入折叠面板，默认不出现在主屏（放在 .card 之后才能覆盖 display） */
.ds--full .chart-grid > .card.card--rate,
.ds--full .chart-grid > .card.card--praise {
  display: none;
}

/* 标题区与图表区用细线分开，标题加粗放大 */
.ds--full .card-head {
  align-items: flex-start;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.ds--full .card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: .2px;
}

.ds--full .card-title::before {
  content: '';
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ffb066, #ff7a1a);
}

.ds--full .card-sub {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--text-3);
}

.ds--full .chart-body { flex: 1; min-height: 0; }

/* ---------- 大屏：更多指标入口 ---------- */
.ds-more {
  display: flex;
  justify-content: center;
  padding-top: 2px;
}

.ds-more-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 30px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  box-shadow: var(--shadow-xs);
  transition: all .18s ease;
}

.ds-more-btn:hover {
  border-color: var(--brand-line);
  transform: translateY(-1px);
}

.ds-more-btn.open {
  background: var(--brand-soft);
  border-color: var(--brand-line);
}

.ds-more-title { font-size: 12.5px; font-weight: 800; color: var(--text-1); }
.ds-more-hint { font-size: 12px; font-weight: 500; color: var(--text-3); }
.ds-more-arrow { font-size: 12px; font-weight: 700; color: var(--brand); }

/* ---------- 大屏：更多指标抽屉 ---------- */
.ds-drawer {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 64px;
  z-index: 40;
  display: flex;
  flex-direction: column;
  max-height: 62vh;
  padding: 14px 16px 16px;
  border-radius: 18px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: 0 20px 52px rgba(16, 24, 40, .2);
}

.ds-drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ds-drawer-title {
  font-size: 14px;
  font-weight: 800;
  color: var(--text-1);
}

.ds-drawer-close {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-2);
  cursor: pointer;
  transition: all .16s ease;
}

.ds-drawer-close:hover {
  color: var(--brand);
  border-color: var(--brand-line);
}

.ds-drawer-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.ds-drawer-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 46vh;
  padding: 14px 16px 10px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--bg);
}

.ds-drawer-card .card-head {
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.ds-drawer-card .card-title { font-size: 14px; font-weight: 800; }
.ds-drawer-card .card-sub { display: block; margin-top: 3px; font-size: 11.5px; color: var(--text-3); }
.ds-drawer-card .chart-body { flex: 1; min-height: 0; }

.ds-drawer-enter-active,
.ds-drawer-leave-active {
  transition: opacity .22s ease, transform .22s ease;
}

.ds-drawer-enter-from,
.ds-drawer-leave-to {
  opacity: 0;
  transform: translateY(16px);
}

@keyframes fsIn {
  from { opacity: 0; transform: scale(.985); }
  to { opacity: 1; transform: scale(1); }
}

.ds--loading { cursor: progress; }

/* ===== 顶部横幅 ===== */
.ds-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
  padding: 18px 22px;
  margin-bottom: 14px;
  border-radius: 18px;
  background: linear-gradient(118deg, #2b1a0d 0%, #6b3410 42%, #b4530b 100%);
  box-shadow: 0 10px 30px rgba(180, 83, 11, .22);
  overflow: hidden;
}

.ds-head::after {
  content: '';
  position: absolute;
  right: -60px;
  top: -70px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, .16), transparent 68%);
  pointer-events: none;
}

/* 横幅上的高光扫过 */
.ds-head::before {
  content: '';
  position: absolute;
  top: -40%;
  bottom: -40%;
  width: 38%;
  left: -55%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, .12), transparent);
  transform: skewX(-16deg);
  animation: sweep 7.5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes sweep {
  0% { left: -55%; }
  60%, 100% { left: 125%; }
}

.head-left { position: relative; z-index: 1; }

.head-badge {
  display: inline-block;
  padding: 2px 9px;
  margin-bottom: 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, .16);
  color: #ffd7b0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.6px;
}

.ds-head h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #fff;
  letter-spacing: .6px;
}

.head-sub {
  margin: 7px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, .72);
}

.head-sub b { color: #ffcf9e; font-weight: 700; }

.head-right {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
  padding-right: 14px;
  margin-right: 4px;
  border-right: 1px solid rgba(255, 255, 255, .18);
}

.clock-time {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 0 rgba(74, 222, 128, .65);
  animation: livePulse 2s ease-out infinite;
}

@keyframes livePulse {
  0% { box-shadow: 0 0 0 0 rgba(74, 222, 128, .6); }
  70% { box-shadow: 0 0 0 8px rgba(74, 222, 128, 0); }
  100% { box-shadow: 0 0 0 0 rgba(74, 222, 128, 0); }
}

.clock-date { font-size: 11px; color: rgba(255, 255, 255, .62); }

.range-switch {
  display: flex;
  padding: 3px;
  border-radius: 11px;
  background: rgba(255, 255, 255, .13);
  gap: 2px;
}

.range-btn {
  padding: 7px 13px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: rgba(255, 255, 255, .78);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all .18s ease;
  white-space: nowrap;
}

.range-btn:hover { color: #fff; background: rgba(255, 255, 255, .12); }

.range-btn.active {
  background: #fff;
  color: #b4530b;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .14);
}

.pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: 11px;
  background: rgba(255, 255, 255, .10);
  color: rgba(255, 255, 255, .82);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all .18s ease;
  white-space: nowrap;
}

.pill-btn:hover { background: rgba(255, 255, 255, .2); color: #fff; }

.pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .45);
}

.pill-btn.on {
  background: rgba(255, 255, 255, .95);
  color: #b4530b;
  border-color: #fff;
}

.pill-btn.on .pill-dot {
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, .25);
  animation: pulse 1.8s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .35; }
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: 11px;
  background: rgba(255, 255, 255, .12);
  color: #fff;
  cursor: pointer;
  transition: all .18s ease;
}

.icon-btn:hover { background: rgba(255, 255, 255, .22); }
.icon-btn.spinning svg { animation: spin .9s linear infinite; }

/* 一屏展示按钮 */
.screen-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 13px;
  border: 1px solid rgba(255, 255, 255, .28);
  border-radius: 11px;
  background: rgba(255, 255, 255, .14);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all .18s ease;
}

.screen-btn:hover {
  background: rgba(255, 255, 255, .26);
  transform: translateY(-1px);
}

.screen-btn.on {
  background: #fff;
  border-color: #fff;
  color: #b4530b;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .18);
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ===== KPI ===== */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.kpi {
  position: relative;
  padding: 13px 15px 12px;
  border-radius: 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
  animation: rise .5s cubic-bezier(.22, .8, .3, 1) both;
  animation-delay: calc(var(--i) * 45ms);
}

/* 用 translate 而非 transform 做入场，避免覆盖 hover 的 transform */
@keyframes rise {
  from { opacity: 0; translate: 0 10px; }
  to { opacity: 1; translate: 0 0; }
}

.kpi:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(16, 24, 40, .10);
  border-color: color-mix(in srgb, var(--accent) 34%, var(--border));
}

.kpi:hover .kpi-glow {
  opacity: .24;
  transform: scale(1.15);
}

.kpi-glow {
  position: absolute;
  top: -28px;
  right: -22px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: var(--accent);
  opacity: .12;
  filter: blur(6px);
  transition: opacity .28s ease, transform .28s ease;
  pointer-events: none;
}

.kpi::before {
  content: '';
  position: absolute;
  left: 0;
  top: 16%;
  bottom: 16%;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--accent);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
  min-height: 18px;
}

.kpi-icon { font-size: 17px; line-height: 1; }

.kpi-tag {
  font-size: 10px;
  font-weight: 700;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 13%, transparent);
  padding: 1px 6px;
  border-radius: 5px;
}

.kpi-delta {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 5px;
  letter-spacing: .2px;
}

.kpi-delta.up { color: #d43c3c; background: rgba(212, 60, 60, .10); }
.kpi-delta.down { color: #15803d; background: rgba(21, 128, 61, .10); }

.kpi-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-1);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  letter-spacing: -.4px;
  white-space: nowrap;
}

.kpi-value small {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-3);
  margin-left: 2px;
}

.kpi-label {
  margin-top: 5px;
  font-size: 12px;
  color: var(--text-3);
}

.kpi-extra {
  margin-top: 2px;
  font-size: 10px;
  color: #a9b1bd;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 洞察 ===== */
.insight-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.insight {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  border-radius: 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
  transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease;
  animation: rise .55s cubic-bezier(.22, .8, .3, 1) both;
}

.insight:nth-child(1) { animation-delay: 260ms; }
.insight:nth-child(2) { animation-delay: 320ms; }
.insight:nth-child(3) { animation-delay: 380ms; }
.insight:nth-child(4) { animation-delay: 440ms; }

.insight:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
  border-color: color-mix(in srgb, var(--tone, #ff7a1a) 30%, var(--border));
}

.insight:hover .insight-emoji {
  animation: pop .5s cubic-bezier(.34, 1.56, .64, 1);
}

@keyframes pop {
  0% { transform: scale(1); }
  45% { transform: scale(1.16); }
  100% { transform: scale(1); }
}

.insight::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--tone, #ff7a1a);
}

.insight-emoji {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: color-mix(in srgb, var(--tone, #ff7a1a) 12%, transparent);
  font-size: 17px;
}

.insight-body { min-width: 0; flex: 1; }

.insight-label { font-size: 11px; color: var(--text-3); }

.insight-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.insight-hint {
  font-size: 10px;
  color: #a9b1bd;
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tone-gold { --tone: #f59e0b; }
.tone-green { --tone: #22c55e; }
.tone-blue { --tone: #3b82f6; }
.tone-red { --tone: #ef4444; }
.tone-orange { --tone: #ff7a1a; }

/* ===== 图表网格 ===== */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 12px;
}

.card {
  padding: 14px 16px 10px;
  border-radius: 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-xs);
  transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease;
  animation: rise .6s cubic-bezier(.22, .8, .3, 1) both;
}

.chart-grid .card:nth-child(1) { animation-delay: 120ms; }
.chart-grid .card:nth-child(2) { animation-delay: 190ms; }
.chart-grid .card:nth-child(3) { animation-delay: 260ms; }
.chart-grid .card:nth-child(4) { animation-delay: 330ms; }
.chart-grid .card:nth-child(5) { animation-delay: 400ms; }
.chart-grid .card:nth-child(6) { animation-delay: 470ms; }
.chart-grid .card:nth-child(7) { animation-delay: 540ms; }

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(16, 24, 40, .09);
  border-color: var(--border-strong);
}

.span-4 { grid-column: span 4; }
.span-6 { grid-column: span 6; }
.span-8 { grid-column: span 8; }
.span-12 { grid-column: span 12; }

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.head-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
}

.card-sub {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  color: var(--text-3);
}

.legend-inline {
  display: flex;
  align-items: center;
  font-size: 11px;
  color: var(--text-3);
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
}

.mini-seg {
  display: flex;
  padding: 2px;
  border-radius: 8px;
  background: #f1f4f8;
  gap: 2px;
}

.mini-seg button {
  padding: 3px 9px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-3);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all .16s ease;
}

.mini-seg button:hover { color: var(--text-1); }

.mini-seg button.active {
  background: #fff;
  color: var(--brand-dark);
  box-shadow: 0 1px 3px rgba(16, 24, 40, .10);
}

.filter-chip {
  padding: 4px 11px;
  border: 1px solid var(--brand-line);
  border-radius: 12px;
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all .16s ease;
  white-space: nowrap;
  animation: chipIn .24s cubic-bezier(.34, 1.56, .64, 1);
}

@keyframes chipIn {
  from { opacity: 0; transform: scale(.84); }
  to { opacity: 1; transform: scale(1); }
}

.filter-chip:hover {
  background: #ffe7d2;
  border-color: var(--brand);
}

.ds-foot {
  margin-top: 14px;
  padding: 4px 2px 0;
  font-size: 11px;
  color: #a9b1bd;
  text-align: right;
}

.dot-sep { margin: 0 6px; }

@media (max-width: 1360px) {
  .kpi-grid { grid-template-columns: repeat(3, 1fr); }
  .insight-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 1180px) {
  .span-8, .span-4 { grid-column: span 12; }
  .span-6 { grid-column: span 6; }
}

@media (max-width: 900px) {
  .span-6 { grid-column: span 12; }
}

@media (max-width: 720px) {
  .ds { padding: 12px; }
  .ds-head { padding: 14px 16px; }
  .ds-head h1 { font-size: 19px; }
  .clock { display: none; }
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .kpi-value { font-size: 20px; }
}

/* 尊重系统「减少动态效果」偏好 */
@media (prefers-reduced-motion: reduce) {
  .ds,
  .ds--full,
  .ds-head::before,
  .kpi,
  .insight,
  .card,
  .filter-chip,
  .live-dot,
  .pill-btn.on .pill-dot {
    animation: none !important;
  }
}
</style>
