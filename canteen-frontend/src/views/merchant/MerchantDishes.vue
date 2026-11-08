<template>
  <div class="page-container adaptive-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="manage-header">
          <span style="font-weight: 600">菜品管理</span>
          <div class="batch-toolbar">
            <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
            <span class="selected-hint">已选 {{ selectedRows.length }} 项</span>
            <el-button type="danger" plain :disabled="!selectedRows.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
            <el-button type="primary" @click="openCreate">＋ 新增菜品</el-button>
          </div>
        </div>
      </template>

      <div ref="wrapRef" class="adaptive-table">
      <el-table ref="tableRef" :data="records" row-key="id" border stripe v-loading="loading" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column label="图片" width="70" align="center">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" style="width: 44px; height: 44px; border-radius: 6px" fit="cover" />
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜名" min-width="140" />
        <el-table-column label="价格" width="110" align="center">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 1 ? '在售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜品' : '新增菜品'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="菜名" prop="name">
          <el-input v-model="form.name" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="价格（元）" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 160px" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" maxlength="30" placeholder="如：荤菜 / 素菜 / 汤品" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="菜品图片">
          <el-upload
            :show-file-list="false"
            :http-request="doUpload"
            accept="image/*"
          >
            <div v-if="form.image" class="img-upload-box">
              <el-image :src="form.image" style="width: 96px; height: 96px; border-radius: 8px" fit="cover" />
              <span class="img-upload-tip">点击更换</span>
            </div>
            <div v-else class="img-upload-box img-upload-empty">
              <span style="font-size: 26px">＋</span>
              <span class="img-upload-tip">上传图片</span>
            </div>
          </el-upload>
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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { pageDishes, createDish, updateDish, deleteDish } from '@/api/dish'
import { uploadImage } from '@/api/sales'
import { useAdaptivePaging } from '@/composables/useAdaptivePaging'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const total = ref(0)
const dialogVisible = ref(false)
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

const query = reactive({ current: 1, size: 10, stallId: null })
const form = reactive({ id: null, name: '', price: 1, category: '', description: '', image: '', status: 1, stallId: null })
const formRef = ref()
const rules = {
  name: [{ required: true, message: '请填写菜名', trigger: 'blur' }],
  price: [{ required: true, message: '请填写价格', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    query.stallId = userStore.userInfo?.stallId
    const res = await pageDishes(query)
    records.value = res.data.records || []
    total.value = res.data.total || 0
    selectedRows.value = []
    nextTick(() => tableRef.value?.clearSelection())
  } finally {
    loading.value = false
  }
}

const { bind } = useAdaptivePaging(query, loadData)

function openCreate() {
  Object.assign(form, { id: null, name: '', price: 1, category: '', description: '', image: '', status: 1, stallId: userStore.userInfo?.stallId })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  Object.assign(form, { ...row, stallId: userStore.userInfo?.stallId })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function doUpload(option) {
  try {
    const res = await uploadImage(option.file)
    form.image = res.data
    ElMessage.success('图片已上传')
  } catch {
    ElMessage.error('图片上传失败')
  }
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

function toggleStatus(row) {
  updateDish(row.id, { ...row, stallId: userStore.userInfo?.stallId, status: row.status === 1 ? 0 : 1 })
    .then(() => {
      ElMessage.success('已更新')
      loadData()
    })
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
  await loadData()
  await nextTick()
  bind(wrapRef.value)
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

.img-upload-box {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.img-upload-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--border);
  color: var(--text-3);
}

.img-upload-tip {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  text-align: center;
  padding: 2px 0;
}

.img-upload-empty .img-upload-tip {
  position: static;
  background: none;
  color: var(--text-3);
}

.muted {
  color: var(--text-3);
}
</style>
