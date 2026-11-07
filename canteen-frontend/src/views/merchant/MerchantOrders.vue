<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span style="font-weight: 600">订单处理</span>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
            <el-option label="待取餐" value="PENDING" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
        </div>
      </template>

      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="菜品" min-width="220">
          <template #default="{ row }">
            <div v-for="it in row.items" :key="it.id" class="order-item-line">
              {{ it.dishName }} × {{ it.quantity }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="下单人" width="100" />
        <el-table-column label="金额" width="90" align="center">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="取餐时间" width="170" align="center">
          <template #default="{ row }">{{ row.pickupTime }}</template>
        </el-table-column>
        <el-table-column label="支付" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 'PAID' ? 'success' : 'info'" size="small" effect="plain">
              {{ row.payStatusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" @click="setStatus(row, 'DONE')">出餐完成</el-button>
              <el-button link type="danger" @click="setStatus(row, 'CANCELED')">取消</el-button>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 12px">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageStallOrders, updateOrderStatus } from '@/api/order'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, status: '' })

function statusType(status) {
  return status === 'DONE' ? 'success' : status === 'CANCELED' ? 'info' : 'warning'
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageStallOrders(query)
    records.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function setStatus(row, status) {
  const tip = status === 'DONE' ? '确认该订单已出餐完成？' : '确定取消该订单？'
  ElMessageBox.confirm(tip, '提示', { type: 'warning' })
    .then(async () => {
      await updateOrderStatus(row.id, status)
      ElMessage.success('已更新')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.order-item-line {
  font-size: 12px;
  line-height: 1.6;
}

.muted {
  color: var(--text-3);
}
</style>
