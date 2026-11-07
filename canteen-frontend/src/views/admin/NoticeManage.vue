<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">公告管理</span>
          <el-button type="primary" @click="openCreate">＋ 发布公告</el-button>
        </div>
      </template>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listNotices, createNotice, updateNotice, deleteNotice } from '@/api/notice'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const dialogVisible = ref(false)

const form = reactive({ id: null, title: '', content: '', status: 1 })

const formRef = ref()
const rules = {
  title: [
    { required: true, message: '请填写公告标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过100字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请填写公告内容', trigger: 'blur' },
    { max: 2000, message: '内容不能超过2000字', trigger: 'blur' }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const res = await listNotices()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, title: '', content: '', status: 1 })
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
      await updateNotice(form.id, form)
    } else {
      await createNotice(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteNotice(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>
