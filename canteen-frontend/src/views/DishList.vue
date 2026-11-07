<template>
  <div class="page-container">
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
          <el-button size="small" @click="$router.push(stall?.area === 'north' ? '/stalls/north' : '/stalls/south')">返回档口</el-button>
          <el-button type="warning" size="small" @click="openCart">购物车结算</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索菜名 / 简介 / 档口"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
        <el-select v-model="query.stallId" placeholder="全部档口" clearable style="width: 170px" @change="onStallChange">
          <el-option v-for="s in stalls" :key="s.id" :label="`${s.windowNo}号 ${s.name}`" :value="s.id" />
        </el-select>
        <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 130px" @change="loadData">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.sortBy" placeholder="排序方式" style="width: 150px" @change="loadData">
          <el-option label="评分优先" value="rating" />
          <el-option label="评价数优先" value="reviews" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="最新上架" value="newest" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
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
import { ref, reactive, watch, onMounted } from 'vue'
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

const query = reactive({
  current: 1,
  size: 12,
  keyword: '',
  stallId: route.query.stallId ? Number(route.query.stallId) : undefined,
  category: undefined,
  sortBy: 'rating'
})

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

function resetQuery() {
  query.keyword = ''
  query.stallId = undefined
  query.category = undefined
  query.sortBy = 'rating'
  query.current = 1
  stall.value = null
  loadData()
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

onMounted(async () => {
  const [catRes, stallRes] = await Promise.all([listCategories(), listStalls()])
  categories.value = catRes.data
  stalls.value = stallRes.data
})
</script>

<style scoped>
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
