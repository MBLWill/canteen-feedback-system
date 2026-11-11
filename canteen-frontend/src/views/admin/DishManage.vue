<template>
  <div class="page-container adaptive-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">菜品管理</span>
          <el-button type="primary" @click="openCreate">＋ 新增菜品</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索菜名" clearable style="width: 200px" @keyup.enter="loadData" />
        <el-select v-model="query.stallId" placeholder="全部档口" clearable style="width: 170px" @change="loadData">
          <el-option v-for="s in stalls" :key="s.id" :label="`${s.windowNo}号 ${s.name}`" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <div class="filter-toolbar">
          <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
          <span class="selected-hint">已选 {{ selectedRows.length }} 项</span>
          <el-button type="danger" plain :disabled="!selectedRows.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <div ref="wrapRef" class="adaptive-table">
      <el-table ref="tableRef" :data="records" row-key="id" border stripe v-loading="loading" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column label="图" width="70" align="center">
          <template #default="{ row }">
            <img v-if="row.image" :src="row.image" class="dish-thumb" loading="lazy" alt="">
            <span v-else class="dish-thumb dish-thumb-empty">{{ row.name.slice(0, 1) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜品名称" min-width="130" />
        <el-table-column label="所属档口" min-width="140">
          <template #default="{ row }">{{ row.stallName }}（{{ row.windowNo }}号）</template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column label="价格" width="90" align="right">
          <template #default="{ row }">
            <span class="dish-price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100" align="center">
          <template #default="{ row }">{{ row.avgRating }} ({{ row.reviewCount }})</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在售' : '已下架' }}
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
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜品' : '新增菜品'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="所属档口" prop="stallId">
          <el-select v-model="form.stallId" placeholder="请选择档口" style="width: 100%">
            <el-option v-for="s in stalls" :key="s.id" :label="`${s.windowNo}号 ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="form.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :max="999999" :precision="2" :step="0.5" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="招牌菜">
          <el-switch v-model="form.featured" :active-value="1" :inactive-value="0" active-text="设为招牌" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.image" maxlength="255" placeholder="如 /dishes/1.jpg，留空则显示首字色块" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在售</el-radio>
            <el-radio :value="0">下架</el-radio>
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
import { ref, reactive, computed, onMounted, nextTick , onActivated } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageDishes, listCategories, createDish, updateDish, deleteDish } from '@/api/dish'
import { listStalls } from '@/api/stall'
import { useAdaptivePaging } from '@/composables/useAdaptivePaging'

const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const total = ref(0)
const tableRef = ref()
const wrapRef = ref(null)
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
const categories = ref(['主食', '荤菜', '素菜', '汤品', '小吃', '饮品', '甜品'])

const query = reactive({ current: 1, size: 12, keyword: '', stallId: undefined })

const dialogVisible = ref(false)
const form = reactive({
  id: null,
  stallId: undefined,
  name: '',
  price: 0,
  category: '荤菜',
  image: '',
  featured: 0,
  description: '',
  status: 1
})

const formRef = ref()
const rules = {
  stallId: [{ required: true, message: '请选择所属档口', trigger: 'change' }],
  name: [
    { required: true, message: '请填写菜品名称', trigger: 'blur' },
    { max: 60, message: '菜品名称不能超过60字', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请填写价格', trigger: 'change' },
    { type: 'number', min: 0, message: '价格不能为负数', trigger: 'change' }
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ max: 500, message: '简介不能超过500字', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageDishes(query)
    records.value = res.data.records
    total.value = res.data.total
    selectedRows.value = []
    nextTick(() => tableRef.value?.clearSelection())
  } finally {
    loading.value = false
  }
}

const { bind } = useAdaptivePaging(query, loadData)

function openCreate() {
  Object.assign(form, { id: null, stallId: undefined, name: '', price: 0, category: '荤菜', image: '', description: '', status: 1 })
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
      await updateDish(form.id, form)
    } else {
      await createDish(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除菜品「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteDish(row.id)
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
    const results = await Promise.allSettled(rows.map(row => deleteDish(row.id)))
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
  await loadData()
  await nextTick()
  bind(wrapRef.value)
})
onActivated(loadData)
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

.dish-thumb {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dish-thumb-empty {
  background: linear-gradient(135deg, #ffe1a8, #ff9248);
  color: #fff;
  font-weight: 700;
}
</style>
