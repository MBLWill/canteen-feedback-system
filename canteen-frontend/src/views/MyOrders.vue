<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card" style="margin-bottom: 16px">
      <div class="filter-bar" style="margin-bottom: 0">
        <el-select v-model="query.status" placeholder="全部订单状态" clearable style="width: 150px" @change="loadData">
          <el-option label="待取餐" value="PENDING" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
        <el-select v-model="query.payStatus" placeholder="全部支付状态" clearable style="width: 150px" @change="loadData">
          <el-option label="未支付" value="UNPAID" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </el-card>

    <el-empty v-if="!records.length && !loading" description="还没有订单，去菜品页加购试试" />

    <div v-for="order in records" :key="order.id" class="order-card">
      <div class="order-head">
        <div class="order-tags">
          <span class="order-no">{{ order.orderNo }}</span>
          <el-tag size="small" :type="statusType(order.status)">{{ order.statusLabel }}</el-tag>
          <el-tag v-if="order.payStatusLabel" size="small" effect="plain" :type="payTagType(order.payStatus)">
            {{ order.payStatusLabel }}
          </el-tag>
          <el-tag v-if="order.reviewStatusLabel" size="small" effect="plain" :type="reviewTagType(order.reviewStatus)">
            {{ order.reviewStatusLabel }}
          </el-tag>
        </div>
        <div class="muted" style="font-size: 12px">下单时间：{{ order.createTime }}</div>
      </div>

      <div class="order-line">
        <el-icon :size="13"><Shop /></el-icon>
        {{ order.stallName }}（{{ order.windowNo }} 号窗口）
      </div>
      <div class="order-line">
        <el-icon :size="13"><Clock /></el-icon>
        取餐时间：<b>{{ order.pickupTime }}</b>
      </div>
      <div v-if="order.remark" class="order-line">
        <el-icon :size="13"><ChatLineSquare /></el-icon>
        备注：{{ order.remark }}
      </div>

      <div class="order-items">
        <div v-for="item in order.items || []" :key="item.id" class="order-item">
          <div class="order-item-thumb" :style="{ background: coverGradient(item.category) }">
            <img v-if="item.dishImage && !item.imgBroken" :src="item.dishImage" :alt="item.dishName" @error="item.imgBroken = true">
            <span v-else class="order-item-initial">{{ (item.dishName || '菜').slice(0, 1) }}</span>
          </div>
          <div class="order-item-info">
            <div class="order-item-name">{{ item.dishName }}</div>
            <div class="order-item-price">¥{{ item.price }}</div>
          </div>
          <span class="order-item-qty">×{{ item.quantity }}</span>
          <span class="order-item-amount">¥{{ item.amount }}</span>
        </div>
      </div>

      <div class="order-foot">
        <span>合计 <b class="order-total">¥{{ order.totalAmount }}</b></span>
        <div class="order-actions">
          <el-button v-if="order.payStatus === 'UNPAID'" type="primary" size="small" @click="handlePay(order)">
            去支付
          </el-button>
          <el-button v-if="order.payStatus === 'UNPAID'" link type="primary" @click="refreshPayStatus(order)">
            刷新支付状态
          </el-button>
          <el-button
            v-if="order.reviewStatus === 'UNREVIEWED'"
            type="warning"
            size="small"
            plain
            @click="goReview(order)"
          >
            去评价
          </el-button>
          <el-button v-if="order.status === 'PENDING'" link type="danger" @click="cancelOrder(order)">
            取消订单
          </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMyOrders, updateOrderStatus } from '@/api/order'
import { createPay, getPayStatus, mockPaySuccess } from '@/api/pay'
import { coverGradient } from '@/utils/dishImage'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({ current: 1, size: 10, status: undefined, payStatus: undefined })

function statusType(status) {
  return { PENDING: 'warning', DONE: 'success', CANCELED: 'info' }[status] || 'info'
}

function payTagType(payStatus) {
  return { PAID: 'success', CLOSED: 'info' }[payStatus] || 'warning'
}

function reviewTagType(reviewStatus) {
  return reviewStatus === 'REVIEWED' ? 'success' : 'warning'
}

async function handlePay(order) {
  const res = await createPay(order.id)
  const vo = res.data
  if (vo.paid) {
    ElMessage.success('支付成功')
    loadData()
    return
  }
  if (vo.payUrl) {
    window.open(vo.payUrl, '_blank', 'noopener')
    ElMessage.info('已打开收银台，付完款点「刷新支付状态」确认')
  }
  loadData()
}

async function refreshPayStatus(order) {
  if (!order.payNo) {
    ElMessage.warning('该订单还没有支付单，请先点「去支付」')
    return
  }
  const res = await getPayStatus(order.payNo)
  if (res.data.status === 'PAID') {
    ElMessage.success('已支付')
  } else if (res.data.channel === 'mock') {
    await mockPaySuccess(order.payNo)
    ElMessage.success('模拟支付完成')
  } else {
    ElMessage.info('尚未收到支付结果，请完成付款后再试')
  }
  loadData()
}

function goReview(order) {
  const dishId = order.unreviewedDishIds?.[0]
  if (!dishId) {
    ElMessage.info('订单中的菜品都已评价过啦')
    return
  }
  router.push({ path: `/dishes/${dishId}`, query: { from: 'orders' } })
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageMyOrders(query)
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function cancelOrder(order) {
  ElMessageBox.confirm(`确定取消订单 ${order.orderNo} 吗？`, '取消确认', { type: 'warning' })
    .then(async () => {
      await updateOrderStatus(order.id, 'CANCELED')
      ElMessage.success('订单已取消')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.order-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  margin-bottom: 14px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.order-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.order-no {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  margin-right: 4px;
}

.order-line {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 5px;
}

.order-items {
  margin: 10px 0;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: var(--radius-md, 8px);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.order-item-thumb {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-item-initial {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.order-item-info {
  flex: 1;
  min-width: 0;
}

.order-item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-item-price {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
}

.order-item-qty {
  flex-shrink: 0;
  color: var(--text-2);
}

.order-item-amount {
  color: var(--text-1);
  font-weight: 600;
  min-width: 62px;
  text-align: right;
}

.order-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
  font-size: 13px;
}

.order-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.order-total {
  font-size: 18px;
  color: var(--brand-dark);
}
</style>
