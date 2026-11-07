<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="manage-header">
          <span style="font-weight: 600">首页轮播广告管理</span>
          <div class="batch-toolbar">
            <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
            <span class="selected-hint">已选 {{ selectedRows.length }} 项</span>
            <el-button type="danger" plain :disabled="!selectedRows.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
            <el-button type="primary" @click="openCreate">＋ 新增广告</el-button>
          </div>
        </div>
      </template>

      <el-table ref="tableRef" :data="list" row-key="id" border stripe v-loading="loading" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="sortNo" label="排序" width="70" align="center" />
        <el-table-column label="预览" width="220">
          <template #default="{ row }">
            <div class="banner-preview" :style="previewStyle(row)">
              <span class="banner-preview-emoji">{{ row.emoji || '🍽️' }}</span>
              <span class="banner-preview-title">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="subtitle" label="副标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="关联档口" width="130">
          <template #default="{ row }">{{ row.stallName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="link" label="跳转路径" width="170" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '展示中' : '已下架' }}
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑广告' : '新增广告'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="广告标题" prop="title">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="如：麻辣香锅 · 今日特惠" />
        </el-form-item>
        <el-form-item label="副标题" prop="subtitle">
          <el-input v-model="form.subtitle" maxlength="100" show-word-limit placeholder="如：两人同行第二份半价" />
        </el-form-item>
        <el-form-item label="关联档口">
          <el-select v-model="form.stallId" placeholder="不关联则留空" clearable style="width: 100%">
            <el-option v-for="s in stalls" :key="s.id" :label="`${s.windowNo}号 ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="点击跳转路径" prop="link">
          <el-input v-model="form.link" maxlength="255" placeholder="如：/dishes?stallId=4" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="背景色">
              <el-color-picker v-model="form.bgColor" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="图标" prop="emoji">
              <el-input v-model="form.emoji" maxlength="10" placeholder="🍲" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号" prop="sortNo">
              <el-input-number v-model="form.sortNo" :min="0" :max="9999" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片地址（留空则使用背景色渐变）" prop="image">
          <el-input v-model="form.image" maxlength="255" placeholder="如：https://xxx.com/banner.jpg" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">展示中</el-radio>
            <el-radio :value="0">已下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageBanners, createBanner, updateBanner, deleteBanner } from '@/api/banner'
import { listStalls } from '@/api/stall'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const stalls = ref([])
const dialogVisible = ref(false)
const tableRef = ref()
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

const emptyForm = {
  id: null,
  title: '',
  subtitle: '',
  stallId: undefined,
  image: '',
  bgColor: '#ff7a1a',
  emoji: '🍽️',
  link: '',
  sortNo: 10,
  status: 1
}
const form = reactive({ ...emptyForm })

const formRef = ref()
const rules = {
  title: [
    { required: true, message: '请填写广告标题', trigger: 'blur' },
    { max: 60, message: '广告标题不能超过60字', trigger: 'blur' }
  ],
  subtitle: [{ max: 100, message: '副标题不能超过100字', trigger: 'blur' }],
  emoji: [{ max: 10, message: '图标最多10个字符', trigger: 'blur' }],
  link: [{ max: 255, message: '跳转路径过长', trigger: 'blur' }],
  image: [{ max: 255, message: '图片地址过长', trigger: 'blur' }],
  sortNo: [{ type: 'number', min: 0, max: 9999, message: '排序号范围为0-9999', trigger: 'change' }]
}

function previewStyle(row) {
  if (row.image) return { backgroundImage: `url(${row.image})` }
  const base = row.bgColor || '#ff7a1a'
  return { backgroundImage: `linear-gradient(120deg, ${base}, rgba(0,0,0,0.55))` }
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageBanners({ current: 1, size: 100 })
    list.value = res.data.records
    selectedRows.value = []
    nextTick(() => tableRef.value?.clearSelection())
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateBanner(form.id, form)
    } else {
      await createBanner(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除广告「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteBanner(row.id)
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
    const results = await Promise.allSettled(rows.map(row => deleteBanner(row.id)))
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
  const res = await listStalls()
  stalls.value = res.data
  loadData()
})
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

.banner-preview {
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  color: #fff;
  background-size: cover;
  background-position: center;
}

.banner-preview-emoji {
  font-size: 20px;
  margin-right: 8px;
}

.banner-preview-title {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
