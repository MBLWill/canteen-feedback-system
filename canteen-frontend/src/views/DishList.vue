<template>
  <div class="food-plaza">
    <!-- ============ 页头 ============ -->
    <div class="plaza-header">
      <h1 class="page-title">美食探索</h1>
      <div class="page-crumb">首页 / 食堂 / 美食探索</div>
    </div>

    <!-- ============ 主体：顶行(轮播+AI助手) + 内容行(筛选+菜品 / 今日最佳+人气榜) ============ -->
    <div class="plaza-body">
      <!-- 顶行：轮播图（左） + AI 助手（右）等高 -->
      <div class="plaza-top">
        <BannerCarousel variant="hero" class="plaza-banner" />

        <!-- 板块2：AI 助手推荐 -->
        <div class="side-card side-card--ai">
          <AiRecommend :dishes="rankList" />
        </div>
      </div>

      <!-- ============ 板块1：筛选行（位于轮播图下方，全宽） ============ -->
      <div class="filter-row">
        <!-- 食堂筛选下拉 -->
        <div class="canteen-filter">
          <span class="canteen-filter-label">食堂</span>
          <el-dropdown trigger="click" @command="onCanteenCommand">
            <div class="canteen-select">
              <span class="canteen-select-text">{{ canteenText }}</span>
              <el-icon class="canteen-select-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="all">全部食堂</el-dropdown-item>
                <el-dropdown-item command="south:1" :class="{ 'is-active': query.area === 'south' && query.floor === 1 }">
                  南区食堂 · 1 楼
                </el-dropdown-item>
                <el-dropdown-item command="south:2" :class="{ 'is-active': query.area === 'south' && query.floor === 2 }">
                  南区食堂 · 2 楼
                </el-dropdown-item>
                <el-dropdown-item command="north:1" :class="{ 'is-active': query.area === 'north' }">
                  北区食堂
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <!-- 分类标签 -->
        <div class="category-bar">
          <span class="category-title">
            <span class="title-bar"></span>
            热门菜品
          </span>
          <div class="category-tabs">
            <button
              class="category-tab"
              :class="{ active: !query.category }"
              @click="setCategory(undefined)"
            >
              <el-icon v-if="!query.category" class="tab-icon"><Shop /></el-icon>
              全部
            </button>
            <button
              v-for="c in categories"
              :key="c"
              class="category-tab"
              :class="{ active: query.category === c }"
              @click="setCategory(c)"
            >
              {{ c }}
            </button>
          </div>
        </div>
      </div>

      <!-- 内容行：菜品(左) + 今日最佳/人气榜(右) -->
      <div class="plaza-content">
        <div class="plaza-main">
          <div v-loading="loading" class="dish-grid">
          <DishCard v-for="dish in records" :key="dish.id" :dish="dish" :show-add="false" />
        </div>

        <el-empty v-if="!records.length && !loading" description="暂无菜品" />

        <div v-if="total > query.size" class="plaza-pager">
          <el-pagination
            v-model:current-page="query.current"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[16, 24, 48]"
            layout="total, prev, pager, next"
            background
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
      </div>

      <!-- 右侧：今日最佳(板块3) + 人气榜(板块4) -->
      <div class="plaza-side">
        <!-- 板块3：今日最佳（两道菜） -->
        <div class="side-card side-card--best">
          <div class="side-card-head">
            <span class="side-card-title">今日最佳</span>
            <router-link to="/dishes" class="side-more">查看更多 ›</router-link>
          </div>
          <div v-if="bestDishes.length" class="best-grid">
            <div
              v-for="bd in bestDishes"
              :key="'best-' + bd.dishId"
              class="best-card"
              @click="goDish(bd.dishId)"
            >
              <div class="best-cover" :style="{ background: coverGradient(bd.category) }">
                <img v-if="bd.image" :src="bd.image" :alt="bd.dishName" loading="lazy" @error="$event.target.style.display='none'">
                <span v-else class="best-initial">{{ (bd.dishName || '菜').slice(0, 1) }}</span>
              </div>
              <div class="best-info">
                <div class="best-name">{{ bd.dishName }}</div>
                <div class="best-meta">
                  <span class="best-price">¥{{ priceText(bd.price) }}</span>
                  <span class="best-sales">月售 {{ bd.salesCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="side-empty">暂无数据</div>
        </div>

        <!-- 人气榜 -->
        <div class="side-card">
          <div class="side-card-head">
            <span class="side-card-title">人气榜</span>
            <router-link to="/dishes" class="side-more">查看更多 ›</router-link>
          </div>
          <div class="rank-list">
            <div
              v-for="(r, i) in rankList"
              :key="'rank-' + r.dishId"
              class="rank-item"
              @click="goDish(r.dishId)"
            >
              <span class="rank-badge" :class="'rank-' + (i + 1)">{{ i + 1 }}</span>
              <div class="rank-thumb" :style="{ background: coverGradient(r.category) }">
                <img v-if="r.image" :src="r.image" :alt="r.dishName" loading="lazy" @error="$event.target.style.display='none'">
                <span v-else class="recommend-initial">{{ (r.dishName || '菜').slice(0, 1) }}</span>
              </div>
              <div class="rank-info">
                <div class="rank-name">{{ r.dishName }}</div>
                <div class="rank-rating">
                  <el-icon><StarFilled /></el-icon>
                  <span>{{ r.avgRating ? Number(r.avgRating).toFixed(1) : '—' }}</span>
                </div>
              </div>
              <span class="rank-price">¥{{ priceText(r.price) }}</span>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DishCard from '@/components/DishCard.vue'
import BannerCarousel from '@/components/BannerCarousel.vue'
import AiRecommend from '@/components/AiRecommend.vue'
import { pageDishes, listCategories } from '@/api/dish'
import { getDishRanking } from '@/api/stats'
import { coverGradient } from '@/utils/dishImage'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const records = ref([])
const total = ref(0)
const categories = ref([])
const rankList = ref([])

const query = reactive({
  current: 1,
  size: 24,
  keyword: '',
  stallId: route.query.stallId ? Number(route.query.stallId) : undefined,
  area: normalizeArea(route.query.area),
  floor: normalizeFloor(route.query.floor, route.query.area),
  category: undefined,
  sortBy: 'rating'
})

const canteenText = computed(() => {
  if (!query.area) return '全部食堂'
  if (query.area === 'south') return `南区食堂 · ${query.floor || 1} 楼`
  return '北区食堂'
})

/** 今日最佳：人气榜前两名，展示两道菜 */
const bestDishes = computed(() => (rankList.value || []).slice(0, 2))

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

async function loadRanking() {
  try {
    const res = await getDishRanking(10)
    rankList.value = res.data || []
  } catch {
    rankList.value = []
  }
}

function onCanteenCommand(cmd) {
  if (cmd === 'all') {
    applyArea('', null)
  } else if (cmd.startsWith('south:')) {
    applyArea('south', Number(cmd.split(':')[1]))
  } else if (cmd.startsWith('north:')) {
    applyArea('north', 1)
  }
}

function setCategory(category) {
  query.category = category
  query.current = 1
  loadData()
}

function applyArea(area, floor) {
  query.area = area
  query.floor = floor
  query.current = 1
  loadData()
}

function goDish(id) {
  if (id) router.push(`/dishes/${id}`)
}

function priceText(p) {
  if (p == null) return '—'
  return Number(p).toFixed(2)
}

function normalizeArea(value) {
  return value === 'north' || value === 'south' ? value : ''
}

function normalizeFloor(floorValue, area) {
  if (area === 'north') return 1
  if (area === 'south') {
    return Number(floorValue) === 2 ? 2 : 1
  }
  return null
}

watch(
  () => route.query.stallId,
  async value => {
    query.stallId = value ? Number(value) : undefined
    query.current = 1
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

onMounted(async () => {
  const [catRes] = await Promise.all([listCategories(), loadRanking()])
  categories.value = catRes.data
  loadData()
})
</script>

<style scoped>
.food-plaza {
  padding: 18px 22px 24px;
}

/* 页头 */
.plaza-header {
  margin-bottom: 14px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--text-1);
  line-height: 1.2;
}

.page-crumb {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3);
}

/* 筛选行：下滑浏览菜品时钉在顶部，其他模块位置不动 */
.filter-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 0;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--bg);
  padding: 8px 0;
}

.canteen-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.canteen-filter-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.canteen-select {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 160px;
  padding: 8px 16px;
  border-radius: 8px;
  background: var(--surface);
  border: 1px solid var(--border-strong);
  cursor: pointer;
  transition: all 0.18s ease;
}

.canteen-select:hover {
  border-color: var(--brand);
}

.canteen-select-text {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.canteen-select-arrow {
  color: var(--text-3);
}

/* 分类标签 */
.category-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 800;
  color: var(--text-1);
  flex-shrink: 0;
}

.title-bar {
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ff9a3d, var(--brand));
  display: inline-block;
}

.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.category-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
}

.tab-icon {
  font-size: 14px;
}

.category-tab:hover {
  border-color: var(--brand-line);
  color: var(--brand-dark);
}

.category-tab.active {
  background: linear-gradient(135deg, #ff8f3c, var(--brand));
  border-color: var(--brand);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255, 122, 26, 0.25);
}

/* 轮播 */
.plaza-banner {
  min-width: 0;
}

/* 主体两栏 */
.plaza-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 顶行：轮播 + AI 助手等高 */
.plaza-top {
  display: flex;
  align-items: stretch;
  gap: 18px;
}

.plaza-top .plaza-banner {
  flex: 1;
  min-width: 0;
}

.plaza-top .side-card--ai {
  width: clamp(280px, 24vw, 400px);
  flex-shrink: 0;
}

/* 内容行：筛选+菜品(左) + 今日最佳/人气榜(右) */
.plaza-content {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.plaza-main {
  flex: 1;
  min-width: 0;
}

/* 菜品网格：紧凑多列 */
.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(144px, 1fr));
  gap: 14px;
  min-height: 120px;
}

.plaza-pager {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 右侧栏 */
.plaza-side {
  width: clamp(280px, 24vw, 400px);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-self: stretch;
}

.side-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 14px;
  box-shadow: var(--shadow-xs);
}

/* 板块2：AI 助手 —— 高度与轮播图一致（由顶行 flex 拉伸撑满） */
.side-card--ai {
  display: flex;
  align-items: stretch;
  box-sizing: border-box;
  overflow: hidden;
}

/* 今日最佳与人气榜紧凑排列，不拉开间隔 */
.plaza-side .side-card:last-child {
  margin-top: 0;
}

.side-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.side-card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
}

.side-more {
  font-size: 12px;
  color: var(--text-3);
  text-decoration: none;
  cursor: pointer;
}

.side-more:hover {
  color: var(--brand);
}

.side-empty {
  font-size: 13px;
  color: var(--text-3);
  text-align: center;
  padding: 16px 0;
}

/* 今日最佳：两道菜两列 */
.best-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

/* 今日最佳大图卡片 */
.best-card {
  cursor: pointer;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border);
}

.best-cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.best-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  inset: 0;
}

.best-initial {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  position: relative;
  z-index: 1;
}

.best-info {
  padding: 10px 12px;
  background: var(--surface);
}

.best-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.best-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
}

.best-price {
  font-size: 16px;
  font-weight: 800;
  color: var(--brand);
}

.best-sales {
  font-size: 12px;
  color: var(--text-3);
}

/* 人气榜 */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 4px;
  border-radius: 10px;
  transition: background 0.15s ease;
}

.rank-item:hover {
  background: var(--brand-soft);
}

.rank-badge {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  background: #cbd0d8;
}

.rank-badge.rank-1 { background: linear-gradient(135deg, #ff6a00, #ff9a3d); }
.rank-badge.rank-2 { background: linear-gradient(135deg, #ff9a3d, #ffb870); }
.rank-badge.rank-3 { background: linear-gradient(135deg, #ffb870, #ffd0a3); }

.rank-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.rank-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  inset: 0;
}

.recommend-initial {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-rating {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-top: 2px;
  font-size: 12px;
  color: #f59e0b;
  font-weight: 600;
}

.rank-price {
  font-size: 13px;
  font-weight: 700;
  color: var(--brand);
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .plaza-side,
  .side-card--ai {
    display: none;
  }
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
