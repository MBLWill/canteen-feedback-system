<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">学生评价</span>
          <el-button size="small" @click="handleFilterChange">刷新</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-select
          v-model="query.rating"
          placeholder="全部星级"
          clearable
          style="width: 130px"
          @change="handleFilterChange"
        >
          <el-option v-for="n in 5" :key="n" :label="`${n} 星`" :value="n" />
        </el-select>
        <el-select
          v-model="query.handled"
          placeholder="全部状态"
          clearable
          style="width: 140px"
          @change="handleFilterChange"
        >
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
        <el-button type="primary" @click="handleFilterChange">查询</el-button>
      </div>

      <el-empty v-if="!records.length && !loading" :description="emptyText" />

      <div v-for="r in records" :key="r.id" class="review-item">
        <div class="review-head">
          <div class="review-title">
            <span class="dish-name">{{ r.dishName }}</span>
            <StarRating :value="r.rating" readonly />
            <span class="rating-text">{{ r.rating }} 星</span>
          </div>
          <el-tag :type="r.merchantReply ? 'success' : 'warning'" size="small" effect="plain">
            {{ r.merchantReply ? '已回复' : '待处理' }}
          </el-tag>
        </div>

        <div class="review-meta">
          <span>{{ r.anonymous === 1 ? '匿名学生' : (r.userName || '学生') }}</span>
          <span>{{ r.createTime }}</span>
        </div>

        <div class="review-content">{{ r.content || '（未填写文字评价）' }}</div>

        <div v-if="r.merchantReply" class="review-reply">
          <span class="reply-label">商家回复（{{ r.merchantReplyTime }}）</span>
          <div class="reply-text">{{ r.merchantReply }}</div>
        </div>

        <div class="review-actions">
          <el-button type="primary" plain size="small" @click="openReply(r)">
            {{ r.merchantReply ? '修改回复' : '回复' }}
          </el-button>
        </div>
      </div>

      <div v-if="total > 0" style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="replyVisible" title="回复学生评价" width="520px">
      <div v-if="currentReview" class="dialog-review">
        <span class="dialog-dish">{{ currentReview.dishName }}</span>
        <span class="dialog-content">{{ currentReview.content || '（未填写文字评价）' }}</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import StarRating from '@/components/StarRating.vue'
import { getMerchantReviews, replyReview } from '@/api/review'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, rating: undefined, handled: undefined })

const replyVisible = ref(false)
const replying = ref(false)
const replyText = ref('')
const currentReview = ref(null)

const emptyText = computed(() => {
  const hasRating = query.rating !== undefined && query.rating !== null && query.rating !== ''
  const hasHandled = query.handled !== undefined && query.handled !== null && query.handled !== ''
  if (hasRating || hasHandled) {
    return '没有符合筛选条件的评价'
  }
  return '还没有学生评价，继续保持好口碑吧～'
})

async function loadData() {
  loading.value = true
  try {
    const res = await getMerchantReviews(query)
    records.value = res.data.records || []
    total.value = res.data.total || 0
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
</script>

<style scoped>
.review-item {
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.review-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.dish-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
}

.rating-text {
  font-size: 12px;
  color: var(--text-3);
}

.review-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--text-3);
}

.review-content {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-1);
  white-space: pre-wrap;
}

.review-reply {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.reply-label {
  font-size: 12px;
  color: var(--text-3);
}

.reply-text {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-1);
  white-space: pre-wrap;
}

.review-actions {
  margin-top: 10px;
  text-align: right;
}

.dialog-review {
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.dialog-dish {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  margin-right: 8px;
}

.dialog-content {
  font-size: 13px;
  color: var(--text-2);
}
</style>
