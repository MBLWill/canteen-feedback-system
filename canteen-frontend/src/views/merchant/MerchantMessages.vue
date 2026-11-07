<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <template #header><span style="font-weight: 600">给管理员留言</span></template>
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        placeholder="如：申请延长营业时间、报修设备、反映问题等"
      />
      <div style="margin-top: 10px; text-align: right">
        <el-button type="primary" :loading="sending" @click="handleSend">发送留言</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header><span style="font-weight: 600">历史留言</span></template>
      <el-empty v-if="!messages.length && !loading" description="还没有留言" />
      <div v-for="m in messages" :key="m.id" class="msg-item">
        <div class="msg-head">
          <span class="msg-time">{{ m.createTime }}</span>
          <el-tag :type="m.status === 2 ? 'success' : 'info'" size="small" effect="plain">{{ m.statusLabel }}</el-tag>
        </div>
        <div class="msg-body">{{ m.content }}</div>
        <div v-if="m.reply" class="msg-reply">
          <span class="msg-reply-label">管理员回复（{{ m.replyTime }}）</span>
          <div class="msg-reply-text">{{ m.reply }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendMessage, listMyMessages } from '@/api/merchant'

const content = ref('')
const sending = ref(false)
const loading = ref(false)
const messages = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await listMyMessages()
    messages.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleSend() {
  if (!content.value.trim()) {
    ElMessage.warning('请填写留言内容')
    return
  }
  sending.value = true
  try {
    await sendMessage({ content: content.value.trim() })
    ElMessage.success('留言已发送')
    content.value = ''
    loadData()
  } finally {
    sending.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.msg-item {
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
}

.msg-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.msg-time {
  font-size: 12px;
  color: var(--text-3);
}

.msg-body {
  font-size: 14px;
  color: var(--text-1);
  white-space: pre-wrap;
}

.msg-reply {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.msg-reply-label {
  font-size: 12px;
  color: var(--text-3);
}

.msg-reply-text {
  font-size: 14px;
  color: var(--text-1);
  margin-top: 4px;
}
</style>
