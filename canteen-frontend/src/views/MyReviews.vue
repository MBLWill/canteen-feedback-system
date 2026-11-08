<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="manage-header">
          <span style="font-weight: 600">我的评价记录</span>
          <div class="batch-toolbar">
            <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
            <span class="selected-hint">已选 {{ selectedIds.length }} 项</span>
            <el-button type="danger" plain :disabled="!selectedIds.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!records.length && !loading" description="你还没有评价过菜品，快去逛逛吧～" />

      <div v-for="r in records" :key="r.id" class="review-item">
        <div class="review-row">
          <el-checkbox v-model="selectedIds" :value="r.id" class="item-check" />
          <!-- 菜品图片 -->
          <div class="review-thumb">
            <img v-if="r.dishImage" :src="r.dishImage" :alt="r.dishName" loading="lazy" @error="onImgError($event, r)" />
            <span v-else class="review-initial">{{ (r.dishName || '菜').slice(0, 1) }}</span>
          </div>
          <div class="review-main">
            <div class="review-dish-row">
              <span class="review-dish-name">{{ r.dishName }}</span>
              <el-tag size="small" type="info" effect="plain">{{ r.stallName }}</el-tag>
            </div>
            <div class="muted" style="font-size: 12px">{{ r.createTime }}</div>
            <div style="margin-top: 6px">{{ r.content || '（未填写文字评价）' }}</div>
          </div>
          <div class="review-right">
            <StarRating :value="r.rating" readonly />
            <div class="review-actions">
              <el-tag v-if="r.status === 2" type="danger" size="small">已被屏蔽</el-tag>
              <el-tag v-else-if="r.status === 0" type="warning" size="small">待审核</el-tag>
              <el-tag v-else type="success" size="small">已发布</el-tag>
              <el-button link type="danger" style="margin-left: 8px" @click="handleDelete(r)">删除</el-button>
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
import { pageMyReviews, deleteReview } from '@/api/review'

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

const query = reactive({ current: 1, size: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await pageMyReviews(query)
    records.value = res.data.records
    total.value = res.data.total
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

function onImgError(e, r) {
  e.target.style.display = 'none'
  e.target.nextElementSibling && (e.target.nextElementSibling.style.display = 'flex')
}

function handleDelete(row) {
  ElMessageBox.confirm('删除后不可恢复，确定删除这条评价吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteReview(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

async function handleBatchDelete() {
  const ids = selectedIds.value
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 项吗？删除后不可恢复`, '提示', { type: 'warning' })
  } catch {
    return
  }
  batchDeleting.value = true
  try {
    const results = await Promise.allSettled(ids.map(id => deleteReview(id)))
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

.review-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.item-check {
  flex-shrink: 0;
  margin-top: 14px;
}

.review-thumb {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffd0a6, #ff9d5c);
}

.review-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.review-initial {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.review-main {
  flex: 1;
  min-width: 0;
}

.review-dish-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.review-dish-name {
  font-weight: 600;
  color: var(--text-1);
}

.review-right {
  flex-shrink: 0;
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.review-actions {
  display: flex;
  align-items: center;
}

.review-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f1f3;
}

.review-item:last-child {
  border-bottom: none;
}
</style>
