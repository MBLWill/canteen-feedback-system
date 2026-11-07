<template>
  <div class="page-container">
    <div class="metric-row">
      <div class="metric-card">
        <div class="metric-value">{{ stats.total || 0 }}</div>
        <div class="metric-label">累计提问</div>
      </div>
      <div class="metric-card">
        <div class="metric-value">{{ stats.hitRate ?? 0 }}%</div>
        <div class="metric-label">命中率</div>
      </div>
      <div class="metric-card">
        <div class="metric-value warn">{{ stats.missCount || 0 }}</div>
        <div class="metric-label">未命中</div>
      </div>
      <div class="metric-card">
        <div class="metric-value">{{ stats.satisfaction ?? '—' }}<span v-if="stats.satisfaction">%</span></div>
        <div class="metric-label">回答满意度</div>
      </div>
    </div>

    <el-card shadow="never" class="page-card">
      <template #header>
        <span style="font-weight: 600">未命中问题 TOP10（可补录进知识库）</span>
      </template>
      <el-table :data="missList" border stripe v-loading="missLoading">
        <el-table-column prop="question" label="学生提问" min-width="260" show-overflow-tooltip />
        <el-table-column prop="count" label="提问次数" width="110" align="center" />
        <el-table-column prop="lastTime" label="最近提问" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="toFaq(row)">补录为问答</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="page-card" style="margin-top: 16px">
      <template #header>
        <span style="font-weight: 600">问答记录</span>
      </template>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="question" label="提问" min-width="220" show-overflow-tooltip />
        <el-table-column label="提问人" width="130">
          <template #default="{ row }">{{ row.realName || row.username || '—' }}</template>
        </el-table-column>
        <el-table-column label="命中意图" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="row.hit === 1 ? 'success' : 'danger'">
              {{ row.intentLabel || (row.hit === 1 ? '已回答' : '未命中') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户评价" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.helpful === 1" style="color: #17803d">有帮助</span>
            <span v-else-if="row.helpful === 0" style="color: #b42318">没帮助</span>
            <span v-else style="color: var(--text-3)">未评价</span>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="createTime" label="提问时间" width="170" />
      </el-table>

      <div style="margin-top: 14px; display: flex; justify-content: flex-end">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       :current-page="current" :page-size="size" @current-change="changePage" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getChatLogs, getChatMissTop, getChatStats } from '@/api/chat'

const router = useRouter()
const stats = ref({})
const missList = ref([])
const list = ref([])
const total = ref(0)
const current = ref(1)
const size = 10
const loading = ref(false)
const missLoading = ref(false)

async function loadStats() {
  const res = await getChatStats()
  stats.value = res.data || {}
}

async function loadMiss() {
  missLoading.value = true
  try {
    const res = await getChatMissTop(10)
    missList.value = res.data || []
  } finally {
    missLoading.value = false
  }
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await getChatLogs({ current: current.value, size })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  current.value = page
  loadLogs()
}

function toFaq(row) {
  router.push({ path: '/admin/faqs', query: { question: row.question } })
}

onMounted(() => {
  loadStats()
  loadMiss()
  loadLogs()
})
</script>

<style scoped>
.metric-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.metric-card {
  background: var(--surface);
  border-radius: var(--radius);
  padding: 14px 16px;
  box-shadow: var(--shadow-xs);
}
.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-1);
}
.metric-value.warn { color: var(--brand-deep); }
.metric-label {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}
</style>
