<template>
  <div class="page-container" v-loading="loading">
    <el-button class="back-btn" round @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      {{ fromOrders ? '返回我的订单' : '返回菜品列表' }}
    </el-button>

    <el-card shadow="never" class="page-card" style="margin-bottom: 16px" v-if="detail">
      <el-row :gutter="24">
        <el-col :span="7">
          <div class="dish-image-placeholder" :style="{ background: coverGradient(detail.dish.category) }">
            <img
              v-if="detail.dish.image && !broken"
              class="dish-photo"
              :src="detail.dish.image"
              :alt="detail.dish.name"
              @error="broken = true"
            >
            <span v-else>{{ detail.dish.name.slice(0, 1) }}</span>
          </div>
        </el-col>
        <el-col :span="17">
          <h2 class="page-title">{{ detail.dish.name }}</h2>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="所属档口">
              {{ detail.dish.stallName }}（{{ detail.dish.windowNo }}号窗口）
            </el-descriptions-item>
            <el-descriptions-item label="菜品分类">{{ detail.dish.category || '未分类' }}</el-descriptions-item>
            <el-descriptions-item label="价格">
              <span class="dish-price">¥{{ detail.dish.price }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="累计评价">{{ detail.dish.reviewCount }} 条</el-descriptions-item>
            <el-descriptions-item label="菜品简介" :span="2">
              {{ detail.dish.description || '暂无简介' }}
            </el-descriptions-item>
          </el-descriptions>

          <div style="margin-top: 16px; display: flex; align-items: center; gap: 16px">
            <div>
              <div class="stat-label">综合评分</div>
              <div class="stat-value">{{ detail.dish.avgRating }}</div>
              <StarRating :value="Number(detail.dish.avgRating)" readonly />
            </div>
            <el-divider direction="vertical" style="height: 60px" />
            <div>
              <div class="stat-label">好评率</div>
              <div class="stat-value" style="color: #34c759">{{ detail.dish.goodRate }}%</div>
            </div>
            <div style="flex: 1"></div>
            <div style="display: flex; gap: 8px">
              <el-button
                :type="detail.dish.favorited ? 'warning' : 'default'"
                @click="toggleFavorite"
              >
                {{ detail.dish.favorited ? '★ 已收藏' : '☆ 收藏' }}
              </el-button>
              <el-button type="primary" :loading="adding" @click="handleAddCart">
                加入购物车
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 我要评价 -->
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <template #header><span style="font-weight: 600">✍️ 我要评价</span></template>
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-position="top">
        <el-form-item label="评分" prop="rating">
          <StarRating v-model:value="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="说说这道菜的口味、分量、性价比……"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="reviewForm.anonymous">匿名评价</el-checkbox>
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评价</el-button>
      </el-form>
    </el-card>

    <!-- 评分分布 + 全部评价 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="page-card">
          <template #header><span style="font-weight: 600">评分分布</span></template>
          <EChart :option="distributionOption" height="240px" />
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never" class="page-card">
          <template #header><span style="font-weight: 600">最新评价</span></template>
          <el-empty v-if="!detail.latestReviews?.length" description="还没有人评价，来做第一个吧～" />
          <div v-for="r in detail.latestReviews || []" :key="r.id" class="review-item">
            <div style="display: flex; justify-content: space-between">
              <span style="font-weight: 600">{{ r.userName }}</span>
              <StarRating :value="r.rating" readonly />
            </div>
            <div class="muted" style="font-size: 12px; margin: 4px 0">{{ r.createTime }}</div>
            <div>{{ r.content || '（该用户未填写文字评价）' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EChart from '@/components/EChart.vue'
import StarRating from '@/components/StarRating.vue'
import { getDishDetail } from '@/api/dish'
import { submitReview as submitReviewApi } from '@/api/review'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { addToCart } from '@/api/cart'
import { coverGradient } from '@/utils/dishImage'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const detail = ref(null)
const broken = ref(false)

const reviewForm = reactive({
  dishId: Number(route.params.id),
  rating: 5,
  content: '',
  anonymous: false
})

const reviewFormRef = ref()
const reviewRules = {
  rating: [{ type: 'number', required: true, min: 1, max: 5, message: '评分范围为1-5星', trigger: 'change' }],
  content: [{ max: 500, message: '评价内容不能超过500字', trigger: 'blur' }]
}

const distributionOption = computed(() => {
  const d = detail.value?.distribution || {}
  const rows = [
    { name: '5星', value: d.star5 || 0, color: '#34c759' },
    { name: '4星', value: d.star4 || 0, color: '#8ddc6b' },
    { name: '3星', value: d.star3 || 0, color: '#ffa94d' },
    { name: '2星', value: d.star2 || 0, color: '#ff8a80' },
    { name: '1星', value: d.star1 || 0, color: '#ef4444' }
  ]
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: rows.map(r => r.name) },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f1f3' } } },
    series: [
      {
        type: 'bar',
        barWidth: 22,
        data: rows.map(r => ({ value: r.value, itemStyle: { color: r.color, borderRadius: [4, 4, 0, 0] } })),
        label: { show: true, position: 'top', fontSize: 11 }
      }
    ]
  }
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getDishDetail(route.params.id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

const adding = ref(false)

async function handleAddCart() {
  adding.value = true
  try {
    await addToCart(detail.value.dish.id, 1)
    window.dispatchEvent(new CustomEvent('canteen:cart-changed'))
    ElMessage.success('已加入购物车')
  } finally {
    adding.value = false
  }
}

async function toggleFavorite() {
  const dish = detail.value.dish
  if (dish.favorited) {
    await removeFavorite(dish.id)
    dish.favorited = false
    ElMessage.success('已取消收藏')
  } else {
    await addFavorite(dish.id)
    dish.favorited = true
    ElMessage.success('收藏成功')
  }
}

async function handleSubmit() {
  try {
    await reviewFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await submitReviewApi({ ...reviewForm, anonymous: reviewForm.anonymous ? 1 : 0 })
    ElMessage.success('评价提交成功')
    reviewForm.content = ''
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)

// 页面被 keep-alive 缓存复用，路由参数变化时必须重新加载
watch(() => route.params.id, (id) => {
  if (!id) return
  reviewForm.dishId = Number(id)
  reviewForm.content = ''
  broken.value = false
  loadDetail()
})

// 始终回到当前菜品所属档口的列表，避免 history.back 退回上一道菜
const fromOrders = computed(() => route.query.from === 'orders')

function goBack() {
  if (fromOrders.value) {
    router.push('/my/orders')
    return
  }
  const stallId = detail.value?.dish?.stallId
  router.push(stallId ? `/dishes?stallId=${stallId}` : '/stalls/south')
}
</script>

<style scoped>
.dish-image-placeholder {
  height: 170px;
  border-radius: var(--radius-lg);
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #ffd9a8 0%, #ffb066 55%, #ff9248 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 62px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 3px 10px rgba(180, 70, 0, 0.35);
  box-shadow: var(--shadow-sm);
}

.dish-image-placeholder::after {
  content: '';
  position: absolute;
  width: 160px;
  height: 160px;
  right: -60px;
  top: -58px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
}

.dish-photo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 1;
}

.back-btn {
  margin-bottom: 12px;
  box-shadow: var(--shadow-xs);
}

.back-btn:hover {
  background: var(--brand-soft);
  border-color: var(--brand-line);
  color: var(--brand-dark);
}

.review-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f1f3;
}

.review-item:last-child {
  border-bottom: none;
}
</style>
