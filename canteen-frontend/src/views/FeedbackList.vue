<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <template #header>
        <div class="manage-header">
          <span style="font-weight: 600">
            {{ isAdminView ? '全部留言反馈' : '我的留言反馈' }}
          </span>
          <div class="batch-toolbar">
            <el-checkbox v-model="allSelected" :indeterminate="isIndeterminate">全选</el-checkbox>
            <span class="selected-hint">已选 {{ selectedIds.length }} 项</span>
            <el-button type="danger" plain :disabled="!selectedIds.length" :loading="batchDeleting" @click="handleBatchDelete">批量删除</el-button>
            <el-button type="primary" @click="openCreate">＋ 我要留言</el-button>
          </div>
        </div>
      </template>

      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 130px" @change="loadData">
          <el-option label="建议" value="SUGGEST" />
          <el-option label="投诉" value="COMPLAINT" />
          <el-option label="表扬" value="PRAISE" />
          <el-option label="其他" value="OTHER" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索标题/内容" clearable style="width: 200px" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <el-empty v-if="!records.length && !loading" description="暂无留言" />

      <div v-for="item in records" :key="item.id" class="feedback-item">
        <div class="feedback-row">
          <el-checkbox v-model="selectedIds" :value="item.id" class="item-check" />
          <div class="feedback-main">
            <div style="display: flex; justify-content: space-between; align-items: flex-start">
              <div>
                <span style="font-size: 15px; font-weight: 600">{{ item.title }}</span>
                <el-tag size="small" :type="typeTag(item.type)" effect="plain" style="margin-left: 8px">
                  {{ item.typeLabel }}
                </el-tag>
                <el-tag size="small" :type="statusTag(item.status)" style="margin-left: 6px">
                  {{ item.statusLabel }}
                </el-tag>
              </div>
              <div class="muted" style="font-size: 12px">
                {{ item.userName }} · {{ item.stallName || '未指定档口' }} · {{ item.createTime }}
              </div>
            </div>
            <div style="margin: 8px 0">{{ item.content }}</div>
            <div v-if="item.replyContent" class="reply-box">
              <b>管理员回复：</b>{{ item.replyContent }}
              <div class="muted" style="font-size: 12px; margin-top: 4px">
                {{ item.replyUserName }} · {{ item.replyTime }}
              </div>
            </div>
            <div v-else class="muted" style="font-size: 12px">（等待管理员回复）</div>

            <div style="margin-top: 8px; text-align: right">
              <el-button
                v-if="isAdminView && item.status !== 'RESOLVED'"
                link
                type="success"
                @click="openReply(item)"
              >
                回复并受理
              </el-button>
              <el-button link type="danger" @click="handleDelete(item)">删除</el-button>
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

    <!-- 新增留言 -->
    <el-dialog v-model="createVisible" title="我要留言" width="560px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="关联档口（选填）">
          <el-select v-model="createForm.stallId" placeholder="不指定则发给食堂管理处" clearable style="width: 100%">
            <el-option v-for="s in stalls" :key="s.id" :label="`${s.windowNo}号 ${s.name}`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="createForm.type">
            <el-radio value="SUGGEST">建议</el-radio>
            <el-radio value="COMPLAINT">投诉</el-radio>
            <el-radio value="PRAISE">表扬</el-radio>
            <el-radio value="OTHER">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" maxlength="100" show-word-limit placeholder="一句话概括你的反馈" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 管理员回复 -->
    <el-dialog v-model="replyVisible" title="回复留言" width="560px">
      <div style="margin-bottom: 12px">
        <div style="font-weight: 600">{{ current.title }}</div>
        <div class="muted" style="font-size: 12px">{{ current.content }}</div>
      </div>
      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-position="top">
        <el-form-item label="回复内容" prop="replyContent">
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
        <el-button type="primary" :loading="submitting" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageFeedbacks, createFeedback, replyFeedback, deleteFeedback } from '@/api/feedback'
import { listStalls } from '@/api/stall'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const route = useRoute()

// 后台管理视角（/admin/feedbacks）显示全部留言+回复；日常使用视角（/feedbacks）与学生端一致，仅看自己的留言
const isAdminView = computed(() => route.path.startsWith('/admin'))
const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const total = ref(0)
const stalls = ref([])
const selectedIds = ref([])
const batchDeleting = ref(false)

const allSelected = computed({
  get: () => records.value.length > 0 && selectedIds.value.length === records.value.length,
  set: (val) => {
    selectedIds.value = val ? records.value.map(item => item.id) : []
  }
})
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < records.value.length)

const query = reactive({ current: 1, size: 10, status: undefined, type: undefined, keyword: '' })

const createVisible = ref(false)
const replyVisible = ref(false)
const current = ref({})

const createForm = reactive({ stallId: undefined, type: 'SUGGEST', title: '', content: '' })
const replyForm = reactive({ replyContent: '', status: 'PROCESSING' })

const createFormRef = ref()
const createRules = {
  type: [{ required: true, message: '请选择留言类型', trigger: 'change' }],
  title: [
    { required: true, message: '请填写标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过100字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请填写内容', trigger: 'blur' },
    { max: 1000, message: '内容不能超过1000字', trigger: 'blur' }
  ]
}

const replyFormRef = ref()
const replyRules = {
  replyContent: [
    { required: true, message: '请填写回复内容', trigger: 'blur' },
    { max: 1000, message: '回复内容不能超过1000字', trigger: 'blur' }
  ]
}

function typeTag(type) {
  return { SUGGEST: 'primary', COMPLAINT: 'danger', PRAISE: 'success', OTHER: 'info' }[type] || 'info'
}

function statusTag(status) {
  return { PENDING: 'warning', PROCESSING: 'primary', RESOLVED: 'success' }[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    // 日常使用视角强制只看自己的留言（管理员身份也按 MINE 返回）
    const params = isAdminView.value ? { ...query } : { ...query, scope: 'MINE' }
    const res = await pageFeedbacks(params)
    records.value = res.data.records
    total.value = res.data.total
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.stallId = undefined
  createForm.type = 'SUGGEST'
  createForm.title = ''
  createForm.content = ''
  createVisible.value = true
  createFormRef.value?.clearValidate()
}

function openReply(item) {
  current.value = item
  replyForm.replyContent = ''
  replyForm.status = 'PROCESSING'
  replyVisible.value = true
  replyFormRef.value?.clearValidate()
}

async function submitCreate() {
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await createFeedback(createForm)
    ElMessage.success('留言提交成功，感谢你的反馈！')
    createVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function submitReply() {
  try {
    await replyFormRef.value.validate()
  } catch {
    return
  }
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

function handleDelete(item) {
  ElMessageBox.confirm('确定删除这条留言吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteFeedback(item.id)
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
    const results = await Promise.allSettled(ids.map(id => deleteFeedback(id)))
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

.feedback-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.feedback-main {
  flex: 1;
  min-width: 0;
}

.item-check {
  flex-shrink: 0;
}

.feedback-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f1f3;
}

.feedback-item:last-of-type {
  border-bottom: none;
}

.reply-box {
  background: #f6f9f6;
  border-left: 3px solid #34c759;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
}
</style>
