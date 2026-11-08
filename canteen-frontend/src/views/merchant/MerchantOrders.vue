<template>
  <div class="page-container adaptive-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="page-head">
          <span class="card-title">订单处理</span>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
            <el-option label="待取餐" value="PENDING" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
        </div>
      </template>

      <div ref="wrapRef" class="adaptive-table">
        <el-table :data="records" border stripe v-loading="loading">
          <!-- 展开行：显示订单内每道菜的图片与明细 -->
          <el-table-column type="expand" width="40">
            <template #default="{ row }">
              <div class="order-expand">
                <div v-for="it in row.items" :key="it.id" class="oe-item">
                  <div class="oe-thumb">
                    <span class="oe-initial">{{ (it.dishName || '菜').slice(0, 1) }}</span>
                    <img v-if="it.dishImage" :src="it.dishImage" alt="" @error="$event.target.style.display='none'" />
                  </div>
                  <div class="oe-name">{{ it.dishName }}</div>
                  <div class="oe-spec">¥{{ num(it.price) }} × {{ it.quantity }}</div>
                  <div class="oe-amount">¥{{ num(it.amount) }}</div>
                </div>
                <div v-if="row.remark" class="oe-remark">备注：{{ row.remark }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="orderNo" label="订单号" width="180" />
          <el-table-column label="菜品" min-width="200">
            <template #default="{ row }">
              <span class="dish-summary">{{ summaryText(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="userName" label="下单人" width="100" />
          <el-table-column label="金额" width="90" align="center">
            <template #default="{ row }">¥{{ num(row.totalAmount) }}</template>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageStallOrders, updateOrderStatus } from '@/api/order'
import { useAdaptivePaging } from '@/composables/useAdaptivePaging'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const wrapRef = ref(null)
const query = reactive({ current: 1, size: 10, status: '' })

function statusType(status) {
  return status === 'DONE' ? 'success' : status === 'CANCELED' ? 'info' : 'warning'
}

function num(v) {
  return v == null ? '—' : Number(v).toFixed(2)
}

function summaryText(row) {
  const items = row.items || []
  if (!items.length) return '—'
  const first = items[0].dishName
  const count = items.reduce((s, it) => s + (it.quantity || 0), 0)
  return items.length > 1 ? `${first} 等 ${count} 件` : `${first} × ${items[0].quantity}`
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

const { bind } = useAdaptivePaging(query, loadData)

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

onMounted(async () => {
  await loadData()
  await nextTick()
  bind(wrapRef.value)
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: 600;
  font-size: 16px;
}

.dish-summary {
  color: var(--text-2);
}

.muted {
  color: var(--text-3);
}

/* 展开的订单明细 */
.order-expand {
  padding: 6px 8px 6px 52px;
}

.oe-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border);
}

.oe-item:last-of-type {
  border-bottom: none;
}

.oe-thumb {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffd0a6, #ff9d5c 55%, #f97316);
  overflow: hidden;
  flex-shrink: 0;
}

.oe-thumb .oe-initial {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.oe-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.oe-name {
  flex: 1;
  min-width: 0;
  font-weight: 500;
  color: var(--text-1);
}

.oe-spec {
  width: 140px;
  color: var(--text-3);
  font-size: 13px;
}

.oe-amount {
  width: 90px;
  text-align: right;
  font-weight: 600;
  color: var(--text-1);
}

.oe-remark {
  padding: 8px 0 2px;
  font-size: 12px;
  color: var(--text-3);
}
</style>