<template>
  <div class="page-container adaptive-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">商家留言</span>
          <el-button size="small" @click="loadData">刷新</el-button>
        </div>
      </template>

      <div ref="wrapRef" class="adaptive-table">
      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column label="商家" min-width="230">
          <template #default="{ row }">
            <div class="merchant-cell">
              <div class="merchant-avatar">
                <span class="avatar-fallback">{{ (row.stallName || '商').slice(0, 1) }}</span>
                <img v-if="row.coverImage" :src="row.coverImage" alt="" @error="$event.target.style.display='none'" />
              </div>
              <div class="merchant-info">
                <div class="merchant-name">{{ row.stallName }}</div>
                <div class="merchant-sub">
                  <span v-if="row.windowNo">窗口 {{ row.windowNo }}</span>
                  <span v-if="row.zone">{{ row.zone }}</span>
                  <span v-if="row.managerName">· {{ row.managerName }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="留言内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'info' : 'warning'" size="small" effect="plain">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170" align="center">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="回复" min-width="180">
          <template #default="{ row }">
            <span v-if="row.reply">{{ row.reply }}</span>
            <el-button v-else link type="primary" @click="openReply(row)">回复</el-button>
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

    <el-dialog v-model="replyVisible" title="回复商家留言" width="480px">
      <div class="reply-origin">{{ current.content }}</div>
      <el-input v-model="reply" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="回复内容" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { listAllMessages, replyMessage } from '@/api/merchant'
import { useAdaptivePaging } from '@/composables/useAdaptivePaging'

const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const total = ref(0)
const wrapRef = ref(null)
const query = reactive({ current: 1, size: 10 })
const replyVisible = ref(false)
const reply = ref('')
const current = ref({})

async function loadData() {
  loading.value = true
  try {
    const res = await listAllMessages(query)
    records.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function openReply(row) {
  current.value = row
  reply.value = ''
  replyVisible.value = true
}

async function submitReply() {
  if (!reply.value.trim()) {
    ElMessage.warning('请填写回复内容')
    return
  }
  submitting.value = true
  try {
    await replyMessage(current.value.id, { reply: reply.value.trim() })
    ElMessage.success('已回复')
    replyVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const { bind } = useAdaptivePaging(query, loadData)

onMounted(async () => {
  await loadData()
  await nextTick()
  bind(wrapRef.value)
})
</script>

<style scoped>
.reply-origin {
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.merchant-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.merchant-avatar {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
  overflow: hidden;
  flex-shrink: 0;
}

.merchant-avatar .avatar-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.merchant-avatar img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.merchant-info {
  min-width: 0;
}

.merchant-name {
  font-weight: 600;
  color: var(--text-1);
  line-height: 1.3;
}

.merchant-sub {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
