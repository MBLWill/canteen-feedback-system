<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header><span style="font-weight: 600">用户管理</span></template>

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

      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'success'" size="small" effect="plain">
              {{ row.roleLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="评价数" width="90" align="center">
          <template #default="{ row }">{{ row.reviewCount ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUsers, updateUserStatus } from '@/api/user'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, keyword: '', role: undefined, status: undefined })

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

onMounted(loadData)
</script>
