<template>
  <div class="ds" :class="{ 'ds--full': fullscreen, 'ds--loading': loading }">
    <!-- ===== 顶部横幅 ===== -->
    <header class="ds-head">
      <div class="head-left">
        <h1>食堂运营数据大屏</h1>
        <p class="head-sub">
          数据更新于 <b>{{ data.overview?.dataDate || '—' }}</b>
          · <b>{{ data.overview?.stallCount ?? 0 }}</b> 个档口
          / <b>{{ data.overview?.dishCount ?? 0 }}</b> 道在售菜品
          · <b>{{ data.overview?.reviewCount ?? 0 }}</b> 条评价
          · <b>{{ data.overview?.studentCount ?? 0 }}</b> 位学生
        </p>
      </div>
      <div class="head-right">
        <div class="clock">
          <span class="clock-time"><i class="live-dot" />{{ clock.time }}</span>
          <span class="clock-date">{{ clock.date }}</span>
        </div>
        <div class="range-switch">
          <button v-for="r in ranges" :key="r.value" class="range-btn" :class="{ active: days === r.value }" @click="setDays(r.value)">{{ r.label }}</button>
        </div>
        <button class="pill-btn" :class="{ on: autoRefresh }" @click="toggleAuto">
          <span class="pill-dot" />{{ autoRefresh ? '自动刷新' : '手动' }}
        </button>
        <button class="icon-btn" :class="{ spinning: loading }" title="刷新" @click="loadAll">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-2.6-6.4M21 3v6h-6" /></svg>
        </button>
        <button class="screen-btn" :class="{ on: fullscreen }" @click="toggleFullscreen">
          <svg v-if="!fullscreen" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3H5a2 2 0 0 0-2 2v3M16 3h3a2 2 0 0 1 2 2v3M8 21H5a2 2 0 0 1-2-2v-3M16 21h3a2 2 0 0 0 2-2v-3" /></svg>
          <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 8V5a2 2 0 0 1 2-2h3M21 8V5a2 2 0 0 0-2-2h-3M3 16v3a2 2 0 0 0 2 2h3M21 16v3a2 2 0 0 1-2 2h-3" /></svg>
          <span>{{ fullscreen ? '退出大屏' : '大屏模式' }}</span>
        </button>
      </div>
    </header>

    <!-- ===== KPI ===== -->
    <div class="kpi-grid">
      <div v-for="(k, i) in kpis" :key="k.key" class="kpi" :style="{ '--accent': k.accent, '--i': i }">
        <div class="kpi-top">
          <span class="kpi-icon">{{ k.emoji }}</span>
          <span v-if="k.delta !== null && k.delta !== undefined" class="kpi-delta" :class="k.delta >= 0 ? 'up' : 'down'">{{ k.delta >= 0 ? '▲' : '▼' }} {{ Math.abs(k.delta).toFixed(1) }}%</span>
          <span v-else class="kpi-tag">{{ k.tag }}</span>
        </div>
        <div class="kpi-value">{{ k.value }}<small v-if="k.unit">{{ k.unit }}</small></div>
        <div class="kpi-label">{{ k.label }}</div>
        <div v-if="k.extra" class="kpi-extra">{{ k.extra }}</div>
      </div>
    </div>

    <!-- ===== 中部趋势通栏 ===== -->
    <section class="panel panel--trend">
      <div class="panel-head">
        <div>
          <span class="panel-title">销量与营收趋势</span>
          <span class="panel-sub">近 {{ days }} 天 · 柱=销量(份) / 线=营收(元) · 虚线为日均销量</span>
        </div>
        <div class="legend-inline">
          <i class="dot" style="background:#ff9f45" />销量
          <i class="dot" style="background:#3b82f6; margin-left:12px" />营收
        </div>
      </div>
      <div v-if="data.salesTrend?.length" class="panel-body"><EChart :option="trendOption" :height="chartH('286px')" /></div>
      <el-empty v-else description="暂无销量数据" :image-size="70" />
    </section>

    <!-- ===== 左右两列 ===== -->
    <div class="cols">
      <!-- 左列 -->
      <div class="col">
        <section class="panel">
          <div class="panel-head">
            <div><span class="panel-title">档口销量排行</span><span class="panel-sub">按购买人数排序 · 点击柱子联动菜品榜</span></div>
            <div class="head-tools">
              <div class="mini-seg">
                <button :class="{ active: stallMetric === 'quantity' }" @click="stallMetric = 'quantity'">销量</button>
                <button :class="{ active: stallMetric === 'amount' }" @click="stallMetric = 'amount'">营收</button>
              </div>
              <button v-if="selectedStallId" class="filter-chip" @click="clearStallFilter">{{ selectedStallName }} ×</button>
            </div>
          </div>
          <div v-if="data.stallRank?.length" class="panel-body"><EChart :option="stallRankOption" :height="chartH('300px')" @click="onStallClick" /></div>
          <el-empty v-else description="暂无档口数据" :image-size="70" />
        </section>

        <section class="panel panel--quad">
          <div class="panel-head">
            <div><span class="panel-title">档口经营象限</span><span class="panel-sub">横轴=销量，纵轴=好评率，气泡=营收 · 均值线切四象限</span></div>
          </div>
          <div v-if="quadrantPoints.length" class="panel-body"><EChart :option="quadrantOption" :height="chartH('320px')" @click="onStallClick" /></div>
          <el-empty v-else description="暂无可对比的档口数据" :image-size="70" />
        </section>

        <section class="panel panel--rate">
          <div class="panel-head">
            <div><span class="panel-title">评分分布</span><span class="panel-sub">全校 {{ data.overview?.reviewCount ?? 0 }} 条有效评价 · 平均 {{ Number(data.overview?.avgRating || 0).toFixed(2) }} 分</span></div>
          </div>
          <div v-if="data.ratingDist?.length" class="panel-body"><EChart :option="ratingOption" :height="chartH('300px')" /></div>
          <el-empty v-else description="暂无评分数据" :image-size="70" />
        </section>
      </div>

      <!-- 右列 -->
      <div class="col">
        <section class="panel">
          <div class="panel-head">
            <div><span class="panel-title">{{ dishTitle }}</span><span class="panel-sub">点击柱子查看菜品详情 · 按累计{{ dishMetric === 'amount' ? '营收' : '销量' }}</span></div>
            <div class="mini-seg">
              <button :class="{ active: dishMetric === 'quantity' }" @click="dishMetric = 'quantity'">销量</button>
              <button :class="{ active: dishMetric === 'amount' }" @click="dishMetric = 'amount'">营收</button>
            </div>
          </div>
          <div v-if="filteredDishRank.length" class="panel-body"><EChart :option="dishRankOption" :height="chartH('300px')" @click="onDishClick" /></div>
          <el-empty v-else description="当前筛选下暂无排行数据" :image-size="70" />
        </section>

        <section class="panel panel--category">
          <div class="panel-head">
            <div><span class="panel-title">品类销量占比</span><span class="panel-sub">点击扇区联动菜品排行</span></div>
            <button v-if="selectedCategory" class="filter-chip" @click="selectedCategory = ''">{{ selectedCategory }} ×</button>
          </div>
          <div v-if="data.categorySales?.length" class="panel-body"><EChart :option="categoryOption" :height="chartH('286px')" @click="onCategoryClick" /></div>
          <el-empty v-else description="暂无品类数据" :image-size="70" />
        </section>

        <section class="panel panel--praise">
          <div class="panel-head">
            <div><span class="panel-title">各档口口碑对比</span><span class="panel-sub">好评率 = 4-5 星占比 · 低于 60% 标红</span></div>
          </div>
          <div v-if="data.stallRating?.length" class="panel-body"><EChart :option="goodRateOption" :height="chartH('300px')" /></div>
          <el-empty v-else description="暂无评价数据" :image-size="70" />
        </section>
      </div>
    </div>

    <!-- ===== 底部信息栏 ===== -->
    <footer class="ds-foot">
      数据来源：菜品销量汇总 / 评价表 · 趋势为近 {{ days }} 天，排行与评分为累计口径
      <span class="dot-sep">·</span> 最后刷新 {{ updatedAt || '—' }}
      <span class="dot-sep">·</span> 仅管理员可见
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
const autoRefresh = ref(false)
const updatedAt = ref('')
const selectedStallId = ref(null)
const selectedStallName = ref('')
const selectedCategory = ref('')
/** 排行指标：销量 / 营收（档口榜与菜品榜共用，保持口径一致） */
const stallMetric = ref('quantity')
// 菜品榜口径独立，避免切换档口榜时菜品榜跟着变
const dishMetric = ref('quantity')

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
    // 图例由卡片头部（.legend-inline）承担，图表内不再重复
    legend: { show: false },
    grid: { left: 8, right: 12, top: 16, bottom: 16, containLabel: true },
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
        type: 'value', name: fullscreen.value ? '' : '销量', nameTextStyle: { color: '#98a1ad', fontSize: 12 },
        splitNumber: 4,
        splitLine: { lineStyle: { color: '#f1f4f8' } },
        axisLabel: { color: '#98a1ad', fontSize: 12, hideOverlap: true }
      },
      {
        type: 'value', name: fullscreen.value ? '' : '营收', nameTextStyle: { color: '#98a1ad', fontSize: 12 },
        splitNumber: 4,
        splitLine: { show: false },
        axisLabel: { color: '#98a1ad', fontSize: 12, hideOverlap: true, formatter: v => (v >= 10000 ? (v / 10000).toFixed(1) + 'w' : v) }
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
            // 大屏下顶部指标带已给出「日均销量」，这里不再叠加文字避免与刻度相撞
            show: !fullscreen.value,
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
  const centerX = wide ? '33%' : '50%'
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
          right: 2,
          top: 'middle',
          width: 132,
          itemWidth: 9,
          itemHeight: 9,
          itemGap: 9,
          textStyle: { color: '#5b6472', fontSize: 11 },
          formatter: name => `${name}   ${pctOf(name)}%`
        }
      : {
          bottom: 0, itemWidth: 9, itemHeight: 9, itemGap: 12,
          textStyle: { color: '#5b6472', fontSize: 11 }
        },
    series: [
      {
        type: 'pie',
        radius: wide ? ['46%', '94%'] : ['46%', '68%'],
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
  // 切到营收口径时同步重排，避免条形长度变了但顺序还是销量序
  const top = [...(data.value.stallRank || [])]
    .sort((a, b) => Number(b[stallMetric.value] || 0) - Number(a[stallMetric.value] || 0))
    .slice(0, fullscreen.value ? 8 : 10)
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
      axisLabel: { color: '#5b6472', fontSize: 12, interval: 0, formatter: v => (v.length > (fullscreen.value ? 5 : 6) ? v.slice(0, fullscreen.value ? 5 : 6) + '…' : v) }
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
  return [...list].sort((a, b) => Number(b[dishMetric.value] || 0) - Number(a[dishMetric.value] || 0))
})

const dishTitle = computed(() => {
  if (selectedStallName.value) return `${selectedStallName.value} · 热销菜品`
  if (selectedCategory.value) return `${selectedCategory.value} · 热销菜品`
  return `菜品${dishMetric.value === 'amount' ? '营收' : '销量'} TOP${fullscreen.value ? 8 : 10}`
})

const dishRankOption = computed(() => {
  const all = filteredDishRank.value
  const filtered = !!(selectedStallId.value || selectedCategory.value)
  const limited = filtered ? all : all.slice(0, fullscreen.value ? 8 : 10)
  const list = [...limited].reverse()
  const isAmount = dishMetric.value === 'amount'
  const max = Math.max(1, ...list.map(i => Number(i[dishMetric.value] || 0)))
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
      axisLabel: { color: '#5b6472', fontSize: 12, interval: 0, formatter: v => (v.length > (fullscreen.value ? 6 : 7) ? v.slice(0, fullscreen.value ? 6 : 7) + '…' : v) }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 20,
        animationDelay: idx => idx * 40,
        data: list.map((i, idx) => ({
          value: i[dishMetric.value],
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
  const src = [...(data.value.stallRating || [])].sort((a, b) => a.goodRate - b.goodRate)
  // 大屏下用垂直柱状图（12 档口横向排开，好评率作柱高），彻底避免横向条形标签重叠
  const list = src
  const vertical = fullscreen.value
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
    grid: vertical
      ? { left: 8, right: 12, top: 26, bottom: 4, containLabel: true }
      : { left: 6, right: 58, top: 20, bottom: 10, containLabel: true },
    xAxis: vertical
      ? {
          type: 'category',
          data: list.map(i => i.stallName),
          axisLine: { lineStyle: { color: '#e2e6ec' } },
          axisTick: { show: false },
          axisLabel: { color: '#5b6472', fontSize: 11, interval: 0, rotate: 0, formatter: v => (v.length > 4 ? v.slice(0, 4) + '…' : v) }
        }
      : {
          type: 'value', max: 100,
          splitLine: { lineStyle: { color: '#f1f4f8' } },
          axisLabel: { color: '#98a1ad', fontSize: 12, formatter: '{value}%' }
        },
    yAxis: vertical
      ? {
          type: 'value', max: 100,
          splitNumber: 3,
          splitLine: { lineStyle: { color: '#f1f4f8' } },
          axisLabel: { color: '#98a1ad', fontSize: 11, formatter: '{value}%' }
        }
      : {
          type: 'category',
          data: list.map(i => i.stallName),
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: { color: '#5b6472', fontSize: 12, interval: 0, formatter: v => (v.length > 6 ? v.slice(0, 6) + '…' : v) }
        },
    series: [
      {
        type: 'bar',
        barMaxWidth: vertical ? 26 : 16,
        animationDelay: idx => idx * 30,
        data: list.map(i => ({
          value: i.goodRate,
          itemStyle: {
            borderRadius: vertical ? [6, 6, 0, 0] : [0, 7, 7, 0],
            color: i.goodRate >= 80 ? '#22c55e' : i.goodRate >= 60 ? '#f59e0b' : '#ef4444'
          }
        })),
        label: {
          show: true,
          position: vertical ? 'top' : 'right',
          formatter: p => Number(p.value || 0).toFixed(1) + '%',
          color: '#8a93a0',
          fontSize: 11
        },
        markLine: vertical
          ? {
              silent: true,
              symbol: 'none',
              lineStyle: { type: 'dashed', color: '#c9cfd8', width: 1 },
              label: { formatter: '60%', color: '#98a1ad', fontSize: 10 },
              data: [{ yAxis: 60 }]
            }
          : undefined
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
    grid: { left: 8, right: 34, top: fullscreen.value ? 24 : 30, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      // 大屏下卡片副标题已写明「横轴=销量」，轴名省略避免被裁
      name: fullscreen.value ? '' : '销量(份)',
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
      name: fullscreen.value ? '' : '好评率(%)',
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
watch(fullscreen, v => {
  document.body.style.overflow = v ? 'hidden' : ''
  // 进出大屏后容器尺寸变化，让图表按新尺寸重绘
  requestAnimationFrame(() => window.dispatchEvent(new Event('resize')))
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
/* ===================== 基础 ===================== */
.ds {
  --brand: #ff7a1a;
  --brand-dark: #e35f00;
  padding: 16px 20px 18px;
  min-height: 100%;
  background: var(--bg);
}

/* ===================== 顶部横幅 ===================== */
.ds-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 24px;
  border-radius: 16px;
  background: linear-gradient(120deg, #7b3f00, #a8581a 55%, #c9712e);
  color: #fff;
  box-shadow: 0 8px 24px rgba(123, 63, 0, .22);
  margin-bottom: 16px;
}

.ds-head h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 1px;
  white-space: nowrap;
}

.head-sub {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: rgba(255, 255, 255, .78);
}

.head-sub b {
  color: #ffd7b0;
  font-weight: 700;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding-right: 14px;
  border-right: 1px solid rgba(255, 255, 255, .25);
}

.clock-time {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 0 rgba(74, 222, 128, .7);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(74, 222, 128, .7); }
  70% { box-shadow: 0 0 0 7px rgba(74, 222, 128, 0); }
  100% { box-shadow: 0 0 0 0 rgba(74, 222, 128, 0); }
}

.clock-date {
  font-size: 11px;
  color: rgba(255, 255, 255, .7);
  margin-top: 2px;
}

.range-switch {
  display: flex;
  gap: 4px;
  padding: 3px;
  border-radius: 10px;
  background: rgba(255, 255, 255, .14);
}

.range-btn {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 12.5px;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all .16s ease;
  white-space: nowrap;
}

.range-btn:hover { background: rgba(255, 255, 255, .12); }

.range-btn.active {
  background: #fff;
  color: #b4530b;
  font-weight: 700;
}

.pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, .3);
  background: rgba(255, 255, 255, .12);
  color: #fff;
  font-size: 12.5px;
  padding: 7px 13px;
  border-radius: 10px;
  cursor: pointer;
  white-space: nowrap;
}

.pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9ca3af;
}

.pill-btn.on .pill-dot {
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, .9);
}

.icon-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, .3);
  border-radius: 10px;
  background: rgba(255, 255, 255, .12);
  color: #fff;
  cursor: pointer;
  transition: all .16s ease;
}

.icon-btn:hover { background: rgba(255, 255, 255, .24); }
.icon-btn.spinning svg { animation: spin .9s linear infinite; }

@keyframes spin { to { transform: rotate(360deg); } }

.screen-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: #fff;
  color: #b4530b;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all .16s ease;
}

.screen-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0, 0, 0, .2); }

.screen-btn.on {
  background: rgba(255, 255, 255, .18);
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .4);
}

/* ===================== KPI ===================== */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.kpi {
  position: relative;
  padding: 16px 18px;
  border-radius: 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
  transition: transform .18s ease, box-shadow .18s ease;
}

.kpi::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--accent);
}

.kpi:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(16, 24, 40, .1);
}

.kpi-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.kpi-icon { font-size: 17px; }

.kpi-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent) 14%, #fff);
  color: var(--accent);
  font-weight: 700;
}

.kpi-delta {
  font-size: 11px;
  font-weight: 700;
}

.kpi-delta.up { color: #e11d48; }
.kpi-delta.down { color: #16a34a; }

.kpi-value {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--text-1);
  font-variant-numeric: tabular-nums;
}

.kpi-value small {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-3);
  margin-left: 3px;
}

.kpi-label {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-3);
}

.kpi-extra {
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--text-3);
}

/* ===================== 面板（图表卡） ===================== */
.panel {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 18px 10px;
  border-bottom: 1px solid var(--border);
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
  color: var(--text-1);
}

.panel-title::before {
  content: '';
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ffb066, #ff7a1a);
}

.panel-sub {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--text-3);
}

.panel-body {
  flex: 1;
  min-height: 0;
  padding: 6px 8px 8px;
}

.head-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-seg {
  display: inline-flex;
  gap: 3px;
  padding: 3px;
  border-radius: 9px;
  background: var(--bg, #f5f7fa);
  border: 1px solid var(--border);
}

.mini-seg button {
  border: none;
  background: transparent;
  color: var(--text-2);
  font-size: 12px;
  padding: 4px 11px;
  border-radius: 7px;
  cursor: pointer;
  transition: all .15s ease;
}

.mini-seg button.active {
  background: #fff;
  color: var(--brand);
  font-weight: 700;
  box-shadow: 0 1px 4px rgba(16, 24, 40, .08);
}

.filter-chip {
  padding: 4px 11px;
  border: 1px solid var(--brand-line, #ffd4ae);
  border-radius: 12px;
  background: var(--brand-soft, #fff3e8);
  color: var(--brand-dark);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.legend-inline {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-3);
  white-space: nowrap;
}

.legend-inline .dot {
  width: 9px;
  height: 9px;
  border-radius: 3px;
  display: inline-block;
}

/* ===================== 布局：普通模式（长页） ===================== */
.panel--trend { height: 320px; margin-bottom: 16px; }

.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.col .panel { height: 340px; }
.col .panel--quad,
.col .panel--category { height: 300px; }
.col .panel--rate,
.col .panel--praise { height: 240px; }

.ds-foot {
  margin-top: 16px;
  padding-top: 12px;
  text-align: center;
  font-size: 12px;
  color: var(--text-3);
}

.dot-sep { margin: 0 6px; color: var(--border-strong, #d5dbe3); }

/* ===================== 全屏大屏模式 ===================== */
.ds--full {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  grid-template-rows: auto auto auto 1fr auto;
  gap: 14px;
  padding: 14px 18px 12px;
  overflow: hidden;
  background: var(--bg);
}

.ds--full .ds-head {
  margin-bottom: 0;
  padding: 12px 22px;
}

.ds--full .ds-head h1 { font-size: 22px; }
.ds--full .head-sub { font-size: 12px; }

.ds--full .kpi-grid {
  margin-bottom: 0;
  gap: 12px;
}

.ds--full .kpi { padding: 12px 16px; }
.ds--full .kpi-value { font-size: 24px; }
.ds--full .kpi-extra { display: none; }

.ds--full .panel--trend {
  height: auto;
  flex: 1;
  min-height: 0;
  margin-bottom: 0;
}

.ds--full .cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  min-height: 0;
  overflow: hidden;
}

.ds--full .col {
  display: grid;
  grid-template-rows: 1.25fr 1fr 0.9fr;
  gap: 14px;
  min-height: 0;
}

.ds--full .col .panel,
.ds--full .col .panel--quad,
.ds--full .col .panel--category,
.ds--full .col .panel--rate,
.ds--full .col .panel--praise {
  height: auto;
  min-height: 0;
}

.ds--full .panel-head { padding: 11px 16px 8px; }
.ds--full .panel-title { font-size: 14px; }
.ds--full .panel-body { padding: 4px 6px 6px; }

.ds--full .ds-foot {
  margin-top: 0;
  padding-top: 8px;
  font-size: 11.5px;
}

/* ===================== 响应式 ===================== */
@media (max-width: 1280px) {
  .kpi-value { font-size: 24px; }
  .head-sub { display: none; }
}

@media (max-width: 720px) {
  .ds { padding: 12px; }
  .ds-head { flex-direction: column; align-items: flex-start; }
  .head-right { flex-wrap: wrap; }
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .cols { grid-template-columns: 1fr; }
}
</style>

