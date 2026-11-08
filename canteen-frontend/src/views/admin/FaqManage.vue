<template>
  <div class="page-container adaptive-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">客服知识库</span>
          <el-button type="primary" @click="openCreate">＋ 新增问答</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 150px" @change="loadData">
          <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索问题或答案" clearable style="width: 220px"
                  @keyup.enter="loadData" @clear="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <div class="filter-toolbar">
          <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
          <span class="selected-hint">已选 {{ selectedRows.length }} 项</span>
          <el-button type="danger" plain :disabled="!selectedRows.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <div ref="wrapRef" class="adaptive-table">
      <el-table ref="tableRef" :data="list" row-key="id" border stripe v-loading="loading" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType(row.category)">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="标准问法" min-width="200" />
        <el-table-column prop="answer" label="答案" min-width="300" show-overflow-tooltip />
        <el-table-column prop="keywords" label="匹配关键词" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div class="adaptive-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       :current-page="query.current" :page-size="query.size" @current-change="changePage" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑问答' : '新增问答'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标准问法" prop="question">
          <el-input v-model="form.question" maxlength="200" show-word-limit placeholder="如：怎么投诉某个窗口" />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="5" maxlength="2000" show-word-limit
                    placeholder="给出明确步骤，学生看得懂即可" />
        </el-form-item>
        <el-form-item label="匹配关键词" prop="keywords">
          <el-input v-model="form.keywords" placeholder="逗号分隔，如：投诉,举报,反馈" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortNo" :min="0" :max="999" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                         active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createFaq, deleteFaq, getFaqs, updateFaq } from '@/api/chat'
import { useAdaptivePaging } from '@/composables/useAdaptivePaging'

const route = useRoute()
const categories = [
  { value: 'EVALUATE', label: '评价相关' },
  { value: 'FEEDBACK', label: '投诉与反馈' },
  { value: 'RULE', label: '食堂规则' },
  { value: 'ORDER', label: '订单相关' },
  { value: 'OTHER', label: '其他问题' }
]

const query = reactive({ current: 1, size: 10, category: '', keyword: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const tableRef = ref()
const wrapRef = ref(null)
const selectedRows = ref([])
const batchDeleting = ref(false)

const allSelected = computed({
  get: () => list.value.length > 0 && selectedRows.value.length === list.value.length,
  set: (val) => {
    tableRef.value?.clearSelection()
    if (val) list.value.forEach(row => tableRef.value?.toggleRowSelection(row, true))
  }
})
const isIndeterminate = computed(() => selectedRows.value.length > 0 && selectedRows.value.length < list.value.length)

function onSelectionChange(rows) {
  selectedRows.value = rows
}
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null, category: 'OTHER', question: '', answer: '', keywords: '', sortNo: 50, status: 1
})
const form = reactive(emptyForm())

const rules = {
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  question: [{ required: true, message: '请填写标准问法', trigger: 'blur' }],
  answer: [{ required: true, message: '请填写答案', trigger: 'blur' }],
  keywords: [{ max: 500, message: '关键词不能超过500字', trigger: 'blur' }]
}

function categoryLabel(value) {
  const hit = categories.find(c => c.value === value)
  return hit ? hit.label : value
}

function tagType(value) {
  return { EVALUATE: 'success', FEEDBACK: 'danger', RULE: 'warning', ORDER: 'primary' }[value] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getFaqs({ ...query })
    list.value = res.data.records || []
    total.value = res.data.total || 0
    selectedRows.value = []
    nextTick(() => tableRef.value?.clearSelection())
  } finally {
    loading.value = false
  }
}

const { bind } = useAdaptivePaging(query, loadData)

function changePage(page) {
  query.current = page
  loadData()
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    delete payload.id
    if (form.id) {
      await updateFaq(form.id, payload)
    } else {
      await createFaq(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.question}」吗？`, '提示', { type: 'warning' })
  await deleteFaq(row.id)
  ElMessage.success('已删除')
  loadData()
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
    const results = await Promise.allSettled(rows.map(row => deleteFaq(row.id)))
    const success = results.filter(r => r.status === 'fulfilled').length
    const failed = results.length - success
    if (failed) ElMessage.warning(`成功删除 ${success} 项，${failed} 项删除失败`)
    else ElMessage.success(`成功删除 ${success} 项`)
    await loadData()
  } finally {
    batchDeleting.value = false
  }
}

onMounted(async () => {
  await loadData()
  await nextTick()
  bind(wrapRef.value)
  // 从问答日志页“补录为 FAQ”跳转过来时，预填未命中的提问
  const preset = route.query.question
  if (preset) {
    Object.assign(form, emptyForm(), { question: preset })
    dialogVisible.value = true
  }
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
