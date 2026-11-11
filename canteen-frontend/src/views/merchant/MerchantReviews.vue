<template>
  <div class="page-container review-page">
    <!-- 顶部状态横排 tab -->
    <el-card shadow="never" class="page-card tab-card">
      <div class="status-tabs">
        <div
          v-for="t in statusTabs"
          :key="t.key"
          class="status-tab"
          :class="{ active: activeTab === t.key }"
          @click="switchTab(t)"
        >
          <span class="tab-emoji">{{ t.emoji }}</span>
          <span class="tab-label">{{ t.label }}</span>
        </div>
      </div>

      <div class="filter-bar">
        <el-select v-model="query.sort" placeholder="排序方式" style="width: 150px" @change="handleFilterChange">
          <el-option label="最新优先" value="time_desc" />
          <el-option label="好评优先" value="rating_desc" />
          <el-option label="差评优先" value="rating_asc" />
          <el-option label="最高点赞" value="like_desc" />
          <el-option label="最高浏览" value="view_desc" />
        </el-select>
        <el-select v-model="query.handled" placeholder="全部状态" clearable style="width: 140px" @change="handleFilterChange">
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
        <el-button type="primary" @click="handleFilterChange">查询</el-button>
      </div>
    </el-card>

    <el-empty v-if="!records.length && !loading" :description="emptyText" />

    <template v-else>
      <!-- 顶部：带菜品图片的评价卡片区 -->
      <div v-if="featuredReviews.length" class="featured-section">
        <div class="section-title">精选评价</div>
        <div class="featured-grid">
          <div v-for="r in featuredReviews" :key="'f' + r.id" class="featured-card" @click="openReply(r)">
            <div class="featured-cover" :style="{ background: coverGradient(r.category) }">
              <img v-if="r.dishImage" :src="r.dishImage" :alt="r.dishName" loading="lazy">
              <span v-else class="featured-initial">{{ (r.dishName || '菜').slice(0, 1) }}</span>
              <span class="featured-score">{{ r.rating }}<span class="score-star">★</span></span>
            </div>
            <div class="featured-body">
              <div class="featured-dish">{{ r.dishName }}</div>
              <div class="featured-content">{{ r.content || '（未填写文字评价）' }}</div>
              <div class="featured-foot">
                <div class="featured-user">
                  <img v-if="r.avatar" :src="r.avatar" class="user-avatar">
                  <span v-else class="user-avatar fallback">{{ (r.userName || '学').slice(0, 1) }}</span>
                  <span class="user-name">{{ r.userName || '学生' }}</span>
                </div>
                <el-tag :type="r.merchantReply ? 'success' : 'warning'" size="small" effect="plain">
                  {{ r.merchantReply ? '已回复' : '待处理' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 下方：纯文字评论列表 -->
      <div class="list-section">
        <div class="section-title">其他评论</div>
        <div v-for="r in records" :key="r.id" class="review-item">
          <div class="review-main">
            <img v-if="r.avatar" :src="r.avatar" class="review-avatar">
            <span v-else class="review-avatar fallback">{{ (r.userName || '学').slice(0, 1) }}</span>
            <div class="review-body">
              <div class="review-head">
                <span class="review-name">{{ r.userName || '学生' }}</span>
                <span class="review-dish">{{ r.dishName }}</span>
                <span class="review-time">{{ r.createTime }}</span>
              </div>
              <div class="review-content">{{ r.content || '（未填写文字评价）' }}</div>
              <div class="review-meta">
                <span class="meta-item">👍 {{ r.likeCount || 0 }}</span>
                <span class="meta-item">👁 {{ r.viewCount || 0 }}</span>
                <span class="meta-item">💬 {{ (r.replyCount || 0) + (r.merchantReply ? 1 : 0) }}</span>
              </div>
              <div v-if="r.merchantReply" class="review-reply">
                <span class="reply-label">商家回复（{{ r.merchantReplyTime }}）</span>
                <div class="reply-text">{{ r.merchantReply }}</div>
              </div>
            </div>
          </div>
          <div class="review-side">
            <div class="review-rating">{{ r.rating }}<span class="rating-star">★</span></div>
            <el-button type="primary" plain size="small" @click="openReply(r)">
              {{ r.merchantReply ? '修改回复' : '回复' }}
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <div v-if="total > 0" style="display: flex; justify-content: center; margin-top: 16px">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="replyVisible" title="回复学生评价" width="520px">
      <div v-if="currentReview" class="dialog-review">
        <img v-if="currentReview.dishImage" :src="currentReview.dishImage" class="dialog-img">
        <div>
          <div class="dialog-dish">{{ currentReview.dishName }}</div>
          <div class="dialog-content">{{ currentReview.content || '（未填写文字评价）' }}</div>
        </div>
      </div>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="5"
        maxlength="500"
        show-word-limit
        placeholder="回复学生，例如：感谢反馈，我们会马上改进～"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted , onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchantReviews, replyReview } from '@/api/review'
import { coverGradient } from '@/utils/dishImage'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const activeTab = ref('all')

const statusTabs = [
  { key: 'all', label: '全部', emoji: '☰' },
  { key: 'good', label: '好评', emoji: '👍' },
  { key: 'mid', label: '中评', emoji: '😐' },
  { key: 'bad', label: '差评', emoji: '👎' },
  { key: 'pending', label: '待回复', emoji: '⏳' }
]

const query = reactive({ current: 1, size: 10, rating: undefined, handled: undefined, sort: 'time_desc' })

// 精选区：取当前结果里评分最高（≥4）的带图评价，最多 4 条
const featuredReviews = computed(() => records.value.filter(r => r.rating >= 4).slice(0, 4))

const replyVisible = ref(false)
const replying = ref(false)
const replyText = ref('')
const currentReview = ref(null)

const emptyText = computed(() => {
  const hasFilter = query.rating !== undefined || query.handled !== undefined || activeTab.value !== 'all'
  return hasFilter ? '没有符合筛选条件的评价' : '还没有学生评价，继续保持好口碑吧～'
})

function switchTab(t) {
  activeTab.value = t.key
  // 好评/中评/差评靠前端过滤（拉大 size 保证完整），待回复走后端 handled=0
  if (t.key === 'pending') {
    query.handled = 0
    query.rating = undefined
  } else {
    query.handled = undefined
    query.rating = undefined
  }
  handleFilterChange()
}

async function loadData() {
  loading.value = true
  try {
    // 好评/中评/差评 tab 需要前端精确过滤，拉大 pageSize 保证覆盖全部评价
    const needLocalFilter = ['good', 'mid', 'bad'].includes(activeTab.value)
    const params = { ...query }
    if (needLocalFilter) {
      params.size = 100
    }
    const res = await getMerchantReviews(params)
    let list = res.data.records || []
    if (activeTab.value === 'bad') {
      list = list.filter(r => r.rating <= 2)
    } else if (activeTab.value === 'mid') {
      list = list.filter(r => r.rating === 3)
    } else if (activeTab.value === 'good') {
      list = list.filter(r => r.rating >= 4)
    }
    records.value = list
    total.value = list.length
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  query.current = 1
  loadData()
}

function openReply(row) {
  currentReview.value = row
  replyText.value = row.merchantReply || ''
  replyVisible.value = true
}

async function submitReply() {
  const content = replyText.value.trim()
  if (!content) {
    ElMessage.warning('请填写回复内容')
    return
  }
  replying.value = true
  try {
    await replyReview(currentReview.value.id, { reply: content })
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadData()
  } finally {
    replying.value = false
  }
}

onMounted(loadData)
onActivated(loadData)
</script>

<style scoped>
.review-page {
  width: 100%;
}

.tab-card { margin-bottom: 16px; }

.status-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.status-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 999px;
  background: var(--bg);
  border: 1px solid var(--border);
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  transition: all .18s ease;
}

.status-tab:hover { border-color: var(--brand-line); color: var(--brand); }
.status-tab.active {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
  font-weight: 600;
}

.tab-emoji { font-size: 14px; }

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  margin: 4px 0 12px;
}

.featured-section { margin-bottom: 20px; }

.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.featured-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease;
}

.featured-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.featured-cover {
  position: relative;
  height: 120px;
  overflow: hidden;
}

.featured-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-initial {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  color: #fff;
}

.featured-score {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.65);
  color: #ffb020;
  font-size: 15px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}

.score-star { font-size: 12px; }

.featured-body { padding: 12px 14px; }
.featured-dish {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.featured-content {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 6px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}
.featured-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.featured-user { display: flex; align-items: center; gap: 6px; }
.user-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.user-avatar.fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
  color: #fff;
  font-size: 11px;
}
.user-name { font-size: 12px; color: var(--text-3); }

.list-section { margin-top: 8px; }

.review-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  background: var(--surface);
}

.review-main { display: flex; gap: 12px; flex: 1; min-width: 0; }
.review-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.review-avatar.fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.review-body { flex: 1; min-width: 0; }
.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.review-name { font-size: 14px; font-weight: 600; color: var(--text-1); }
.review-dish {
  font-size: 12px;
  color: var(--brand-dark);
  background: var(--brand-soft);
  padding: 2px 8px;
  border-radius: 999px;
}
.review-time { font-size: 12px; color: var(--text-3); margin-left: auto; }
.review-content {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-1);
  white-space: pre-wrap;
  line-height: 1.7;
}

.review-reply {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
.reply-label { font-size: 12px; color: var(--text-3); }
.reply-text { margin-top: 4px; font-size: 14px; color: var(--text-1); white-space: pre-wrap; }

.review-meta {
  display: flex;
  gap: 14px;
  margin-top: 8px;
}

.meta-item {
  font-size: 12px;
  color: var(--text-3);
}

.review-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}
.review-rating {
  font-size: 20px;
  font-weight: 700;
  color: #f59e0b;
}
.rating-star { font-size: 14px; }

.dialog-review {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
.dialog-img {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.dialog-dish { font-size: 13px; font-weight: 600; color: var(--text-1); }
.dialog-content { font-size: 13px; color: var(--text-2); margin-top: 4px; }

@media (max-width: 900px) {
  .featured-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
