<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="manage-header">
          <span style="font-weight: 600">评价审核管理</span>
          <div class="batch-toolbar">
            <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
            <span class="selected-hint">已选 {{ selectedRows.length }} 项</span>
            <el-button type="danger" plain :disabled="!selectedRows.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
          </div>
        </div>
      </template>

      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索内容/菜品" clearable style="width: 200px" @keyup.enter="loadData" />
        <el-select v-model="query.rating" placeholder="全部星级" clearable style="width: 120px" @change="loadData">
          <el-option :label="`${n} 星`" :value="n" v-for="n in 5" :key="n" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData">
          <el-option label="待审核" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已屏蔽" :value="2" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <el-empty v-if="!records.length && !loading" description="暂无评价" />

      <div v-else v-loading="loading">
        <div v-for="r in records" :key="r.id" class="review-item">
          <el-checkbox v-model="selectedIds" :value="r.id" class="item-check" />
          <div class="review-main">
            <img v-if="r.avatar" :src="r.avatar" class="review-avatar" @error="onAvatarError($event)">
            <span v-else class="review-avatar fallback">{{ (r.userName || '学').slice(0, 1) }}</span>
            <div class="review-body">
              <div class="review-head">
                <span class="review-name">{{ r.userName || '学生' }}</span>
                <span class="review-dish">{{ r.dishName }}</span>
                <el-tag v-if="r.stallName" size="small" type="info" effect="plain">{{ r.stallName }}</el-tag>
                <el-tag size="small" :type="statusTag(r.status)" effect="plain">{{ statusLabel(r.status) }}</el-tag>
                <span class="review-time">{{ r.createTime }}</span>
              </div>
              <div class="review-content">{{ r.content || '（未填写文字评价）' }}</div>
            </div>
          </div>
          <div class="review-side">
            <StarRating :value="r.rating" readonly />
            <div class="review-actions">
              <el-button v-if="r.status !== 1" link type="success" @click="updateStatus(r, 1)">恢复</el-button>
              <el-button v-else link type="warning" @click="updateStatus(r, 2)">屏蔽</el-button>
              <el-button link type="danger" @click="handleDelete(r)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <div style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StarRating from '@/components/StarRating.vue'
import { pageReviews, deleteReview, updateReviewStatus } from '@/api/review'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const selectedIds = ref([])
const batchDeleting = ref(false)

const allSelected = computed({
  get: () => records.value.length > 0 && selectedIds.value.length === records.value.length,
  set: (val) => {
    selectedIds.value = val ? records.value.map(r => r.id) : []
  }
})
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < records.value.length)

const selectedRows = computed(() => records.value.filter(r => selectedIds.value.includes(r.id)))

const query = reactive({ current: 1, size: 10, keyword: '', rating: undefined, status: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = await pageReviews(query)
    records.value = res.data.records
    total.value = res.data.total
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

function statusLabel(status) {
  return { 0: '待审核', 1: '已发布', 2: '已屏蔽' }[status] || '未知'
}

function statusTag(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

function onAvatarError(e) {
  e.target.style.display = 'none'
  const parent = e.target.parentElement
  if (!parent.querySelector('.avatar-fallback')) {
    const span = document.createElement('span')
    span.className = 'avatar-fallback'
    span.textContent = '学'
    parent.appendChild(span)
  }
}

async function updateStatus(row, status) {
  await updateReviewStatus(row.id, status)
  ElMessage.success(status === 1 ? '已恢复显示' : '已屏蔽该评价')
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm('删除后不可恢复，确定删除吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteReview(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

async function handleBatchDelete() {
  const rows = selectedRows.value
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${rows.length} 项吗？删除后不可恢复`, '提示', { type: 'warning' })
  } catch {
    return
  }
  batchDeleting.value = true
  try {
    const results = await Promise.allSettled(rows.map(row => deleteReview(row.id)))
    const success = results.filter(r => r.status === 'fulfilled').length
    const failed = results.length - success
    if (failed) ElMessage.warning(`成功删除 ${success} 项，${failed} 项删除失败`)
    else ElMessage.success(`成功删除 ${success} 项`)
    await loadData()
  } finally {
    batchDeleting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.batch-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.selected-hint {
  font-size: 12px;
  color: var(--text-3);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.review-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  background: var(--surface);
}

.item-check {
  flex-shrink: 0;
  margin-top: 12px;
}

.review-main {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

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

.review-avatar .avatar-fallback {
  font-size: 16px;
  font-weight: 600;
}

.review-body {
  flex: 1;
  min-width: 0;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.review-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.review-dish {
  font-size: 12px;
  color: var(--brand-dark);
  background: var(--brand-soft);
  padding: 2px 8px;
  border-radius: 999px;
}

.review-time {
  font-size: 12px;
  color: var(--text-3);
  margin-left: auto;
}

.review-content {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-1);
  white-space: pre-wrap;
  line-height: 1.7;
}

.review-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.review-actions {
  display: flex;
  align-items: center;
}
</style>
