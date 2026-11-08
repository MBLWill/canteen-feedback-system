<template>
  <div class="page-container page-fill">
    <el-card shadow="never" class="page-card page-card-fill">
      <template #header><span class="card-title">用户管理</span></template>

      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索账号/姓名/学号" clearable style="width: 220px" @keyup.enter="loadData" />
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px" @change="loadData">
          <el-option label="学生" value="STUDENT" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <div ref="wrapRef" class="table-wrap">
        <el-table :data="records" border stripe v-loading="loading">
          <el-table-column prop="username" label="账号" width="130" />
          <el-table-column label="姓名" width="110">
            <template #default="{ row }">
              <div class="user-cell">
                <div class="user-avatar">
                  <span class="avatar-fallback">{{ avatarText(row) }}</span>
                  <img v-if="row.avatar" :src="row.avatar" alt="" @error="$event.target.style.display='none'" />
                </div>
                <span class="user-name">{{ row.realName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'success'" size="small" effect="plain">
                {{ row.roleLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="email" label="绑定邮箱" width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span :class="{ 'email-empty': !row.email }">{{ row.email || '未绑定' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评价数" width="90" align="center">
            <template #default="{ row }">{{ row.reviewCount ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" min-width="170" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button
                :type="row.status === 1 ? 'danger' : 'success'"
                link
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="user-pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUsers, updateUserStatus } from '@/api/user'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const wrapRef = ref(null)

// 行高 / 表头高与下方 CSS 保持一致，按可视高度自动铺满每页条数
const ROW_HEIGHT = 51
const HEADER_HEIGHT = 47
const MIN_SIZE = 10
const MAX_SIZE = 60

const query = reactive({ current: 1, size: MIN_SIZE, keyword: '', role: undefined, status: undefined })

let ro = null

function fitPageSize() {
  const el = wrapRef.value
  if (!el || el.clientHeight <= 0) return
  const rows = Math.max(MIN_SIZE, Math.floor((el.clientHeight - HEADER_HEIGHT) / ROW_HEIGHT))
  const next = Math.min(rows, MAX_SIZE)
  if (next !== query.size) {
    query.size = next
    query.current = 1
    loadData()
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageUsers(query)
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function avatarText(row) {
  const name = row.realName || row.username || '用'
  return String(name).trim().slice(0, 1)
}

function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  ElMessageBox.confirm(`确定${next === 0 ? '禁用' : '启用'}用户「${row.realName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await updateUserStatus(row.id, next)
      ElMessage.success('操作成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(async () => {
  await loadData()
  await nextTick()
  ro = new ResizeObserver(fitPageSize)
  if (wrapRef.value) ro.observe(wrapRef.value)
})

onBeforeUnmount(() => {
  if (ro) ro.disconnect()
})
</script>

<style scoped>
/* 撑满窗口高度，避免大屏下方留白 */
.page-fill {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-card-fill {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.page-card-fill :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

/* 固定行高，配合 fitPageSize 让每页条数铺满可视高度 */
.table-wrap :deep(.el-table__header th.el-table__cell) {
  height: 46px;
}

.table-wrap :deep(.el-table__body td.el-table__cell) {
  height: 50px;
  padding-top: 0;
  padding-bottom: 0;
}

.user-pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  flex-shrink: 0;
}

.card-title {
  font-weight: 600;
  font-size: 16px;
}

/* 头像 + 姓名 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  position: relative;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a4f, #ff6a00);
  overflow: hidden;
  flex-shrink: 0;
}

.user-avatar .avatar-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.user-avatar img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  font-weight: 500;
  color: var(--text-1);
}

.email-empty {
  color: var(--text-3);
}
</style>