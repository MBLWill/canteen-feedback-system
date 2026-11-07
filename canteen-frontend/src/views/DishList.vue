<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card explore-hero">
      <div class="explore-toolbar">
        <!-- 食堂切换（纯文字，无图标框） -->
        <div class="canteen-switch">
          <button
            v-for="c in canteens"
            :key="c.key"
            class="canteen-tab"
            :class="{ active: isCanteenActive(c) }"
            @click="setCanteen(c)"
          >
            <span class="canteen-text">
              <strong>{{ c.label }}</strong>
              <small v-if="c.sub">{{ c.sub }}</small>
            </span>
          </button>

          <!-- 南区楼层切换 -->
          <div v-if="query.area === 'south'" class="floor-switch">
            <button
              v-for="f in [1, 2]"
              :key="f"
              class="floor-tab"
              :class="{ active: query.floor === f }"
              @click="setFloor(f)"
            >
              {{ f }} 楼
            </button>
          </div>
        </div>

        <!-- 筛选栏 -->
        <div class="filter-bar">
          <el-input
            v-model="query.keyword"
            placeholder="搜索菜名 / 简介 / 档口"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
            @clear="loadData"
          />
          <el-select v-model="query.stallId" placeholder="全部档口" clearable style="width: 160px" @change="onStallChange">
            <el-option v-for="s in visibleStalls" :key="s.id" :label="`${s.windowNo}号 ${s.name}`" :value="s.id" />
          </el-select>
          <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 120px" @change="loadData">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
          <el-select v-model="query.sortBy" placeholder="排序方式" style="width: 140px" @change="loadData">
            <el-option label="好评优先" value="rating" />
            <el-option label="销量优先" value="sales" />
            <el-option label="综合排序" value="comprehensive" />
            <el-option label="评价数优先" value="reviews" />
            <el-option label="价格从低到高" value="priceAsc" />
            <el-option label="价格从高到低" value="priceDesc" />
            <el-option label="最新上架" value="newest" />
          </el-select>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="stall" shadow="never" class="page-card" style="margin-bottom: 16px">
      <div class="stall-bar">
        <div class="stall-bar-left">
          <span class="stall-bar-name">{{ stall.name }}</span>
          <el-tag size="small" effect="plain" type="warning">{{ stall.windowNo }} 号窗口</el-tag>
          <span class="muted">{{ stall.location || '位置未标注' }}</span>
          <span class="muted">负责人：{{ stall.managerName || '待分配' }}</span>
          <span class="muted">{{ stall.dishCount }} 道菜 · {{ stall.avgRating }} 分</span>
        </div>
        <div class="stall-bar-right">
          <el-button size="small" @click="clearStall">查看全部菜品</el-button>
          <el-button type="warning" size="small" @click="openCart">购物车结算</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="dish in records" :key="dish.id" :span="6" style="margin-bottom: 16px">
        <DishCard :dish="dish" />
      </el-col>
    </el-row>

    <el-empty v-if="!records.length && !loading" description="暂无菜品" />

    <div style="display: flex; justify-content: center; margin-top: 8px">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[8, 12, 16, 24]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>
  </div>

</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import DishCard from '@/components/DishCard.vue'
import { pageDishes, listCategories } from '@/api/dish'
import { listStalls, getStall } from '@/api/stall'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const categories = ref([])
const stalls = ref([])
const stall = ref(null)

const canteens = [
  { key: 'all', label: '全部食堂', sub: '南北区', area: '', floor: null },
  { key: 'south', label: '南区食堂', sub: '1 楼 · 2 楼', area: 'south', floor: 1 },
  { key: 'north', label: '北区食堂', sub: '', area: 'north', floor: 1 }
]

function isCanteenActive(canteen) {
  if (canteen.key === 'all') return !query.area
  return query.area === canteen.area
}

const query = reactive({
  current: 1,
  size: 12,
  keyword: '',
  stallId: route.query.stallId ? Number(route.query.stallId) : undefined,
  area: normalizeArea(route.query.area),
  floor: normalizeFloor(route.query.floor, route.query.area),
  category: undefined,
  sortBy: 'rating'
})

const visibleStalls = computed(() =>
  stalls.value.filter(s =>
    (!query.area || s.area === query.area) && (!query.floor || s.floor === query.floor)
  )
)

async function loadStall() {
  if (!query.stallId) {
    stall.value = null
    return
  }
  const res = await getStall(query.stallId)
  stall.value = res.data
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageDishes(query)
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openCart() {
  window.dispatchEvent(new CustomEvent('canteen:open-cart'))
}

function onStallChange() {
  loadStall()
  loadData()
}

function clearStall() {
  query.stallId = undefined
  stall.value = null
  loadData()
}

function setCanteen(canteen) {
  if (canteen.key === 'all') {
    applyArea('', null)
  } else if (canteen.key === 'south') {
    // 点击南区默认 1 楼；已在南区时保留当前楼层
    applyArea('south', query.area === 'south' ? query.floor || 1 : 1)
  } else {
    applyArea('north', 1)
  }
}

function setFloor(floor) {
  applyArea('south', floor)
}

function applyArea(area, floor) {
  query.area = area
  query.floor = floor
  query.current = 1
  // 切食堂/楼层时若已选档口不属于当前区域/楼层，清掉档口筛选
  const selected = stalls.value.find(s => s.id === query.stallId)
  const mismatch = selected && (
    (area && selected.area !== area) ||
    (floor && selected.floor !== floor)
  )
  if (mismatch) {
    clearStall()
  } else {
    loadData()
  }
}

function resetQuery() {
  query.keyword = ''
  query.stallId = undefined
  query.area = ''
  query.floor = null
  query.category = undefined
  query.sortBy = 'rating'
  query.current = 1
  stall.value = null
  loadData()
}

function normalizeArea(value) {
  return value === 'north' || value === 'south' ? value : ''
}

function normalizeFloor(floorValue, area) {
  if (area === 'north') return 1
  if (area === 'south') {
    // 南区默认 1 楼，只有明确传 2 时才切到 2 楼
    return Number(floorValue) === 2 ? 2 : 1
  }
  return null
}

watch(
  () => route.query.stallId,
  async value => {
    query.stallId = value ? Number(value) : undefined
    query.current = 1
    await loadStall()
    loadData()
  },
  { immediate: true }
)

watch(
  () => route.query.area,
  value => {
    query.area = normalizeArea(value)
    query.floor = normalizeFloor(route.query.floor, query.area)
    query.current = 1
    loadData()
  }
)

watch(
  () => route.query.floor,
  value => {
    query.floor = normalizeFloor(value, query.area)
    query.current = 1
    loadData()
  }
)

onMounted(async () => {
  const [catRes, stallRes] = await Promise.all([listCategories(), listStalls()])
  categories.value = catRes.data
  stalls.value = stallRes.data
})
</script>

<style scoped>
.explore-hero {
  margin-bottom: 16px;
}

.explore-hero :deep(.el-card__body) {
  padding: 14px 18px;
}

.explore-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.canteen-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.canteen-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.canteen-tab:hover {
  border-color: #ffc69e;
}

.canteen-tab.active {
  background: linear-gradient(135deg, #fff3e8, #fffaf6);
  border-color: var(--brand);
  color: var(--brand-dark);
  box-shadow: 0 4px 12px rgba(255, 106, 0, 0.12);
}

.canteen-text {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  line-height: 1.3;
}

.canteen-text strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
}

.canteen-text small {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-3);
}

.canteen-tab.active .canteen-text strong {
  color: var(--brand-dark);
}

.floor-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: 999px;
  background: #fff8f2;
  border: 1px dashed #ffd2ad;
}

.floor-tab {
  padding: 4px 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: #fff;
  color: var(--text-2);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.floor-tab:hover {
  border-color: #ffc69e;
}

.floor-tab.active {
  background: linear-gradient(135deg, #fff0e0, #ffe4c7);
  border-color: var(--brand);
  color: var(--brand-dark);
  font-weight: 600;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-left: auto;
}

.stall-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.stall-bar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
}

.stall-bar-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-1);
}

.stall-bar-right {
  display: flex;
  gap: 8px;
}
</style>
