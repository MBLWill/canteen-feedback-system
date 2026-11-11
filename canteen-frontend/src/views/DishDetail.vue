<template>
  <div class="page-container" v-loading="loading">
    <el-button class="back-btn" round @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      {{ fromOrders ? '返回我的订单' : '返回菜品列表' }}
    </el-button>

    <el-empty v-if="!detail && !loading" description="菜品不存在或已下架">
      <el-button type="primary" @click="goBack">返回菜品列表</el-button>
    </el-empty>

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

    <!-- 评分分布 + 我要评价 -->
    <el-row :gutter="16" v-if="detail" style="margin-bottom: 16px">
      <el-col :span="8">
        <el-card shadow="never" class="page-card">
          <template #header><span style="font-weight: 600">评分分布</span></template>
          <EChart :option="distributionOption" height="240px" />
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never" class="page-card">
          <template #header><span style="font-weight: 600">✍️ 我要评价</span></template>
          <el-alert
            v-if="!detail.canReview"
            type="info"
            :closable="false"
            show-icon
            title="购买过该菜品后才能评价哦"
            description="先去下单品尝一下，再来分享你的真实感受吧～"
          />
          <el-form v-else ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-position="top">
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
      </el-col>
    </el-row>

    <!-- 全部评价：单独一排 -->
    <el-card shadow="never" class="page-card" v-if="detail">
      <template #header>
        <span style="font-weight: 600">全部评价（{{ detail.latestReviews?.length || 0 }}）</span>
      </template>
      <el-empty v-if="!detail.latestReviews?.length" description="还没有人评价，来做第一个吧～" />
      <div v-for="r in detail.latestReviews || []" :key="r.id" :id="'review-' + r.id" class="review-item">
        <div class="review-head">
          <img
            v-if="r.avatar"
            class="review-avatar"
            :src="r.avatar"
            :alt="r.userName"
            @error="$event.target.style.display = 'none'"
          >
          <div v-else class="review-avatar review-avatar--fallback">{{ (r.userName || '客').slice(0, 1) }}</div>
          <div class="review-head-info">
            <div class="review-name">
              {{ r.userName }}
              <span v-if="r.mine" class="mine-tag">我的评价</span>
            </div>
            <div class="review-time">{{ r.createTime }}</div>
          </div>
          <div class="review-stars">
            <StarRating :value="r.rating" readonly />
          </div>
        </div>

        <div class="review-content">{{ r.content || '（该用户未填写文字评价）' }}</div>

        <div v-if="r.merchantReply" class="merchant-reply">
          <span class="merchant-reply-tag">商家回复</span>{{ r.merchantReply }}
        </div>

        <div class="review-actions">
          <button
            class="review-action-btn"
            :class="{ active: r.liked }"
            @click="toggleLike(r)"
          >
            👍 {{ r.liked ? '已赞' : '点赞' }}<span v-if="r.likeCount"> {{ r.likeCount }}</span>
          </button>
          <span class="review-action-meta">👁 浏览 {{ r.viewCount || 0 }}</span>
          <button class="review-action-btn" @click="toggleReplies(r)">
            💬 回复 {{ (r.replyCount || 0) + (r.merchantReply ? 1 : 0) }}
          </button>
        </div>

        <!-- 回复列表 -->
        <div v-if="r.showReplies" class="reply-list">
          <div v-if="!r.replies || !r.replies.length" class="muted" style="font-size: 12px; padding: 6px 0">
            还没有回复，来问问他吧～
          </div>
          <div v-for="rp in r.replies" :key="rp.id" :id="'reply-' + rp.id" class="reply-item">
            <img
              v-if="rp.avatar"
              class="reply-avatar"
              :src="rp.avatar"
              @error="$event.target.style.display = 'none'"
            >
            <div v-else class="reply-avatar reply-avatar--fallback">{{ (rp.userName || '客').slice(0, 1) }}</div>
            <div class="reply-body">
              <span class="reply-name">{{ rp.userName }}<span v-if="rp.mine" class="mine-tag">我</span></span>
              <span class="reply-content">{{ rp.content }}</span>
            </div>
          </div>
          <div class="reply-input-row">
            <el-input
              v-model="r.replyDraft"
              size="small"
              maxlength="500"
              placeholder="写下你的追问或回答…"
              @keyup.enter="doReply(r)"
            />
            <el-button size="small" type="primary" :loading="r.replying" @click="doReply(r)">回复</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EChart from '@/components/EChart.vue'
import StarRating from '@/components/StarRating.vue'
import { getDishDetail } from '@/api/dish'
import { submitReview as submitReviewApi, likeReview, studentReply, getReplies } from '@/api/review'
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
    // 给每条评价挂上交互用的本地状态
    ;(detail.value.latestReviews || []).forEach(r => {
      r.showReplies = false
      r.replies = []
      r.replyDraft = ''
      r.replying = false
    })
    highlightTarget()
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

// 从通知跳进来时，自动展开并高亮定位到指定评价/回复（?review=评价ID&reply=回复ID）
async function highlightTarget() {
  const reviewId = Number(route.query.review)
  const replyId = Number(route.query.reply)
  if (!reviewId) return
  await nextTick()
  const r = (detail.value?.latestReviews || []).find(x => x.id === reviewId)
  if (!r) return
  r.showReplies = true
  try {
    const res = await getReplies(r.id)
    r.replies = res.data || []
  } catch {
    r.replies = []
  }
  await nextTick()
  // 优先定位具体回复，否则定位整条评价
  const el = document.getElementById(replyId ? `reply-${replyId}` : `review-${reviewId}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('flash-highlight')
    setTimeout(() => el.classList.remove('flash-highlight'), 2400)
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

async function toggleLike(r) {
  try {
    const res = await likeReview(r.id)
    const data = res.data || {}
    r.liked = data.liked
    r.likeCount = data.likeCount
  } catch {
    // 请求层已统一提示
  }
}

async function toggleReplies(r) {
  r.showReplies = !r.showReplies
  if (r.showReplies) {
    try {
      const res = await getReplies(r.id)
      r.replies = res.data || []
    } catch {
      r.replies = []
    }
  }
}

async function doReply(r) {
  const content = (r.replyDraft || '').trim()
  if (!content) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  r.replying = true
  try {
    await studentReply(r.id, { content })
    r.replyDraft = ''
    const res = await getReplies(r.id)
    r.replies = res.data || []
    r.replyCount = (r.replyCount || 0) + 1
  } finally {
    r.replying = false
  }
}

onMounted(loadDetail)

watch(
  () => (route.name === 'DishDetail' ? route.params.id : null),
  (id) => {
    if (!id) return
    reviewForm.dishId = Number(id)
    reviewForm.content = ''
    broken.value = false
    loadDetail()
  }
)

// 已在详情页内时，从通知再次跳转到同一菜品（仅 query 变化）也要重新定位高亮
watch(() => [route.query.review, route.query.reply], () => {
  highlightTarget()
})

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
  padding: 16px 0;
  border-bottom: 1px solid #f0f1f3;
}

.review-item:last-child {
  border-bottom: none;
}

/* 从通知定位跳转时的高亮闪烁 */
.flash-highlight {
  animation: review-flash 1.6s ease;
  border-radius: 8px;
}

@keyframes review-flash {
  0%, 55% {
    background: var(--brand-soft);
    box-shadow: 0 0 0 3px var(--brand-line);
  }
  100% {
    background: transparent;
    box-shadow: none;
  }
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #f3f4f6;
}

.review-avatar--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
}

.review-head-info {
  flex: 1;
  min-width: 0;
}

.review-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.review-time {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
}

.mine-tag {
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: var(--brand);
  background: var(--brand-soft);
}

.review-content {
  margin: 10px 0 8px;
  font-size: 14px;
  color: var(--text-1);
  line-height: 1.6;
}

.merchant-reply {
  margin: 8px 0;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--brand-soft);
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.6;
}

.merchant-reply-tag {
  display: inline-block;
  margin-right: 8px;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: var(--brand);
}

.review-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.review-action-btn {
  border: none;
  background: transparent;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-3);
  cursor: pointer;
  transition: all 0.15s ease;
}

.review-action-btn:hover {
  color: var(--brand);
  background: var(--brand-soft);
}

.review-action-btn.active {
  color: var(--brand);
  font-weight: 600;
}

.review-action-meta {
  font-size: 13px;
  color: var(--text-3);
}

.reply-list {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8f9fa;
}

.reply-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 0;
}

.reply-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.reply-avatar--fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
}

.reply-body {
  flex: 1;
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.5;
}

.reply-name {
  font-weight: 600;
  color: var(--text-1);
  margin-right: 6px;
}

.reply-input-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
