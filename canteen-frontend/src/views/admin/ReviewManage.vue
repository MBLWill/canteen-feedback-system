<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header><span style="font-weight: 600">评价审核管理</span></template>

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
        <div class="filter-toolbar">
          <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
          <span class="selected-hint">已选 {{ selectedRows.length }} 项</span>
          <el-button type="danger" plain :disabled="!selectedRows.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <el-table ref="tableRef" :data="records" row-key="id" border stripe v-loading="loading" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="dishName" label="菜品" min-width="120" />
        <el-table-column prop="stallName" label="档口" width="120" />
        <el-table-column prop="userName" label="评价人" width="110" />
        <el-table-column label="评分" width="150">
          <template #default="{ row }"><StarRating :value="row.rating" readonly /></template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" link type="success" @click="updateStatus(row, 1)">恢复</el-button>
            <el-button v-else link type="warning" @click="updateStatus(row, 2)">屏蔽</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StarRating from '@/components/StarRating.vue'
import { pageReviews, deleteReview, updateReviewStatus } from '@/api/review'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const tableRef = ref()
const selectedRows = ref([])
const batchDeleting = ref(false)

const allSelected = computed({
  get: () => records.value.length > 0 && selectedRows.value.length === records.value.length,
  set: (val) => {
    tableRef.value?.clearSelection()
    if (val) records.value.forEach(row => tableRef.value?.toggleRowSelection(row, true))
  }
})
const isIndeterminate = computed(() => selectedRows.value.length > 0 && selectedRows.value.length < records.value.length)

function onSelectionChange(rows) {
  selectedRows.value = rows
}

const query = reactive({ current: 1, size: 10, keyword: '', rating: undefined, status: undefined })

async function loadData() {
  loading.value = true
  try {
    const res = await pageReviews(query)
    records.value = res.data.records
    total.value = res.data.total
    selectedRows.value = []
    nextTick(() => tableRef.value?.clearSelection())
  } finally {
    loading.value = false
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
.filter-toolbar {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.selected-hint {
  font-size: 12px;
  color: var(--text-3);
}
</style>
