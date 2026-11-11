<template>
  <div class="page-container adaptive-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">订单管理</span>
          <span class="muted" style="font-size: 12px">共 {{ total }} 条订单</span>
        </div>
      </template>

      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
          <el-option label="待取餐" value="PENDING" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
        <el-select v-model="query.stallId" placeholder="全部档口" clearable style="width: 170px" @change="loadData">
          <el-option v-for="s in stalls" :key="s.id" :label="`${s.windowNo}号 ${s.name}`" :value="s.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索订单号 / 学生 / 档口" clearable style="width: 220px" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <div ref="wrapRef" class="adaptive-table">
      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <div v-for="item in row.items || []" :key="item.id" class="expand-item">
                <div class="expand-thumb">
                  <img v-if="item.dishImage" :src="item.dishImage" :alt="item.dishName" @error="onImgError($event)" />
                  <span v-else>{{ (item.dishName || '菜').slice(0, 1) }}</span>
                </div>
                <span class="expand-name">{{ item.dishName }}</span>
                <span class="muted">¥{{ item.price }} × {{ item.quantity }}</span>
                <span class="expand-amount">¥{{ item.amount }}</span>
              </div>
              <div v-if="row.remark" class="expand-remark">备注：{{ row.remark }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userName" label="下单学生" width="110" />
        <el-table-column label="档口" min-width="150">
          <template #default="{ row }">{{ row.stallName }}（{{ row.windowNo }}号）</template>
        </el-table-column>
        <el-table-column prop="pickupTime" label="取餐时间" width="170" />
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">
            <span class="dish-price">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="success" @click="changeStatus(row, 'DONE')">
              标记取餐
            </el-button>
            <el-button v-if="row.status === 'PENDING'" link type="danger" @click="changeStatus(row, 'CANCELED')">
              取消
            </el-button>
            <span v-else class="muted" style="font-size: 12px">已处理</span>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick , onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import { pageOrders, updateOrderStatus } from '@/api/order'
import { listStalls } from '@/api/stall'
import { useAdaptivePaging } from '@/composables/useAdaptivePaging'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const stalls = ref([])
const wrapRef = ref(null)

const query = reactive({ current: 1, size: 10, status: undefined, stallId: undefined, keyword: '' })

function onImgError(e) {
  e.target.style.display = 'none'
  const parent = e.target.parentElement
  const span = document.createElement('span')
  span.textContent = '菜'
  parent.appendChild(span)
}

function statusType(status) {
  return { PENDING: 'warning', DONE: 'success', CANCELED: 'info' }[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageOrders(query)
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const { bind } = useAdaptivePaging(query, loadData)

async function changeStatus(row, status) {
  await updateOrderStatus(row.id, status)
  ElMessage.success(status === 'DONE' ? '已标记为取餐完成' : '订单已取消')
  loadData()
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
.expand-box {
  padding: 8px 16px;
}

.expand-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  padding: 6px 0;
}

.expand-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffd0a6, #ff9d5c);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.expand-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.expand-name {
  flex: 1;
  min-width: 0;
  font-weight: 500;
  color: var(--text-1);
}

.expand-amount {
  color: var(--text-1);
  font-weight: 600;
  min-width: 70px;
  text-align: right;
}

.expand-remark {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-3);
}
</style>
