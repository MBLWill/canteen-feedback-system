<template>
  <div class="page-container adaptive-page">
    <el-card shadow="never" class="page-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索账号 / 姓名 / 档口 / 窗口号"
          clearable
          style="width: 260px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData">
          <el-option label="正常" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <span class="muted total-hint">共 {{ total }} 个商家账号</span>
      </div>

      <div ref="wrapRef" class="adaptive-table">
      <el-table v-loading="loading" :data="records" border stripe row-key="id">
        <el-table-column prop="username" label="商家账号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column label="所属档口" min-width="170">
          <template #default="{ row }">
            <div v-if="row.stallName" class="stall-cell">
              <el-tag size="small" effect="plain" type="warning">{{ row.stallWindowNo }}</el-tag>
              <span class="stall-name">{{ row.stallName }}</span>
            </div>
            <span v-else class="muted">未绑定档口</span>
          </template>
        </el-table-column>
        <el-table-column label="区域 / 楼层" width="140" align="center">
          <template #default="{ row }">
            <span v-if="row.stallArea">
              {{ row.stallArea === 'north' ? '北区食堂' : '南区食堂' }} {{ row.stallFloor }} 楼
            </span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="stallCategory" label="主营" width="100" />
        <el-table-column label="负责人 / 电话" min-width="150">
          <template #default="{ row }">
            <div>{{ row.managerName || '-' }}</div>
            <div class="muted phone">{{ row.managerPhone || row.phone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="dishCount" label="菜品数" width="80" align="center" />
        <el-table-column label="评分" width="80" align="center">
          <template #default="{ row }">
            <span class="rating">{{ row.avgRating || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="salesCount" label="销量" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'primary'"
              :loading="switchingId === row.id"
              @click="toggleStatus(row)"
            >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
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
          background
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMerchants, updateUserStatus } from '@/api/user'
import { useAdaptivePaging } from '@/composables/useAdaptivePaging'

const loading = ref(false)
const switchingId = ref(null)
const records = ref([])
const total = ref(0)
const wrapRef = ref(null)

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: undefined
})

async function loadData() {
  loading.value = true
  try {
    const res = await pageMerchants({
      current: query.current,
      size: query.size,
      keyword: query.keyword.trim() || undefined,
      status: query.status
    })
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const { bind } = useAdaptivePaging(query, loadData)

function resetQuery() {
  query.keyword = ''
  query.status = undefined
  query.current = 1
  loadData()
}

function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 0 ? '禁用' : '启用'
  ElMessageBox.confirm(`确定${action}商家「${row.username}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      switchingId.value = row.id
      try {
        await updateUserStatus(row.id, next)
        ElMessage.success(`${action}成功`)
        await loadData()
      } finally {
        switchingId.value = null
      }
    })
    .catch(() => {})
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  await loadData()
  await nextTick()
  bind(wrapRef.value)
})
</script>

<style scoped>
.total-hint {
  margin-left: auto;
  font-size: 12px;
}

.stall-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stall-name {
  font-weight: 600;
  color: var(--text-1);
}

.phone {
  font-size: 12px;
}

.rating {
  color: #e35f00;
  font-weight: 700;
}
</style>
