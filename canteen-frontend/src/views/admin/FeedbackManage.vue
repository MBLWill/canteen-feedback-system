<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header><span style="font-weight: 600">留言受理与回复</span></template>

      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 120px" @change="loadData">
          <el-option label="建议" value="SUGGEST" />
          <el-option label="投诉" value="COMPLAINT" />
          <el-option label="表扬" value="PRAISE" />
          <el-option label="其他" value="OTHER" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索标题/内容" clearable style="width: 200px" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <div class="filter-toolbar">
          <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
          <span class="selected-hint">已选 {{ selectedRows.length }} 项</span>
          <el-button type="danger" plain :disabled="!selectedRows.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <el-table ref="tableRef" :data="records" row-key="id" border stripe v-loading="loading" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="userName" label="留言人" width="100" />
        <el-table-column prop="stallName" label="关联档口" width="120" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small" effect="plain">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">
              {{ row.replyContent ? '修改回复' : '回复' }}
            </el-button>
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

    <el-dialog v-model="replyVisible" title="回复留言" width="560px">
      <div style="margin-bottom: 12px">
        <div style="font-weight: 600">{{ current.title }}</div>
        <div class="muted" style="font-size: 12px">{{ current.content }}</div>
      </div>
      <el-form :model="replyForm" label-position="top">
        <el-form-item label="回复内容">
          <el-input v-model="replyForm.replyContent" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="更新状态">
          <el-radio-group v-model="replyForm.status">
            <el-radio value="PROCESSING">处理中</el-radio>
            <el-radio value="RESOLVED">已解决</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageFeedbacks, replyFeedback, deleteFeedback } from '@/api/feedback'
import { listStalls } from '@/api/stall'

const loading = ref(false)
const submitting = ref(false)
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
const stalls = ref([])
const replyVisible = ref(false)
const current = ref({})
const replyForm = reactive({ replyContent: '', status: 'PROCESSING' })

const query = reactive({ current: 1, size: 10, status: undefined, type: undefined, keyword: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await pageFeedbacks(query)
    records.value = res.data.records
    total.value = res.data.total
    selectedRows.value = []
    nextTick(() => tableRef.value?.clearSelection())
  } finally {
    loading.value = false
  }
}

function openReply(row) {
  current.value = row
  replyForm.replyContent = row.replyContent || ''
  replyForm.status = row.status === 'RESOLVED' ? 'RESOLVED' : 'PROCESSING'
  replyVisible.value = true
}

async function submitReply() {
  if (!replyForm.replyContent.trim()) return ElMessage.warning('请填写回复内容')
  submitting.value = true
  try {
    await replyFeedback(current.value.id, replyForm)
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除留言「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteFeedback(row.id)
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
    const results = await Promise.allSettled(rows.map(row => deleteFeedback(row.id)))
    const success = results.filter(r => r.status === 'fulfilled').length
    const failed = results.length - success
    if (failed) ElMessage.warning(`成功删除 ${success} 项，${failed} 项删除失败`)
    else ElMessage.success(`成功删除 ${success} 项`)
    await loadData()
  } finally {
    batchDeleting.value = false
  }
}

function typeTag(type) {
  return { SUGGEST: 'primary', COMPLAINT: 'danger', PRAISE: 'success', OTHER: 'info' }[type] || 'info'
}

function statusTag(status) {
  return { PENDING: 'warning', PROCESSING: 'primary', RESOLVED: 'success' }[status] || 'info'
}

onMounted(async () => {
  const res = await listStalls()
  stalls.value = res.data
  loadData()
})
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
