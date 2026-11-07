<template>
  <el-dialog
    v-model="visible"
    title="支付"
    width="380px"
    append-to-body
    :close-on-click-modal="false"
    class="pay-dialog"
  >
    <div class="pay-box">
      <div v-if="pay.totalCount > 1" class="pay-progress">
        第 {{ currentIndex }} / {{ pay.totalCount }} 笔
      </div>
      <div class="pay-amount">¥{{ session?.amount ?? '0.00' }}</div>
      <div class="pay-no">支付单号 {{ session?.payNo }}</div>
      <el-tag size="small" :type="tagType" class="pay-tag">{{ statusLabel }}</el-tag>

      <div v-if="pay.status === 'CREATED' && session?.channel === 'mock'" class="pay-actions">
        <el-button type="success" size="small" :loading="pay.mocking" @click="handleMock">模拟支付成功</el-button>
      </div>
      <div v-if="pay.status === 'CREATED' && session?.payUrl" class="pay-actions">
        <el-button type="primary" size="small" @click="openCashier">打开收银台</el-button>
        <div class="pay-hint">付款后回到本页，点「查询支付结果」自动确认</div>
      </div>
      <div v-if="pay.status === 'PAID' && pay.hasNext" class="pay-hint success">本笔已支付，继续支付下一笔</div>
      <div v-if="pay.status === 'PAID' && !pay.hasNext" class="pay-hint success">全部支付完成，可在「我的订单」查看取餐时间</div>
    </div>

    <template #footer>
      <el-button size="small" @click="close">关闭</el-button>
      <el-button v-if="pay.status === 'PAID' && pay.hasNext" type="primary" size="small" @click="nextPay">下一笔</el-button>
      <el-button v-else-if="pay.status === 'PAID'" type="success" size="small" @click="goOrders">查看我的订单</el-button>
      <el-button v-else type="primary" size="small" :loading="pay.querying" @click="handleQuery">查询支付结果</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePayStore } from '@/store/pay'
import { useCartStore } from '@/store/cart'

const router = useRouter()
const pay = usePayStore()
const cartStore = useCartStore()

const visible = computed({
  get: () => pay.visible,
  set: value => {
    if (!value) pay.dismiss()
  }
})

const session = computed(() => pay.session)

/** 当前是第几笔（1 起），用于多笔进度提示 */
const currentIndex = computed(() => {
  if (!session.value?.payNo || !pay.queue.length) return 1
  const idx = pay.queue.findIndex(s => s.payNo === session.value.payNo)
  return idx === -1 ? 1 : idx + 1
})

const statusLabel = computed(() => {
  if (pay.status === 'PAID') return '已支付'
  if (pay.status === 'CLOSED') return '已关闭'
  return '待支付'
})

const tagType = computed(() => {
  if (pay.status === 'PAID') return 'success'
  if (pay.status === 'CLOSED') return 'info'
  return 'warning'
})

function openCashier() {
  if (session.value?.payUrl) window.open(session.value.payUrl, '_blank', 'noopener')
}

async function handleMock() {
  await pay.mockPay()
}

async function handleQuery() {
  const status = await pay.query()
  // 查单确认已支付后，自动推进到下一笔
  if (status === 'PAID') pay.advance()
}

function nextPay() {
  pay.advance()
}

function close() {
  pay.dismiss()
}

function goOrders() {
  pay.dismiss()
  router.push('/my/orders')
}

// 支付成功：购物车已由后端口径清空，这里同步角标与金额
watch(() => pay.paid, paid => {
  if (paid) {
    cartStore.refresh()
    ElMessage.success('支付完成')
  }
})

onBeforeUnmount(() => pay.stopPoll())
</script>

<style scoped>
.pay-box {
  text-align: center;
  padding: 4px 0 8px;
}

.pay-progress {
  font-size: 12px;
  color: var(--brand);
  font-weight: 600;
  margin-bottom: 6px;
}

.pay-amount {
  font-size: 30px;
  font-weight: 700;
  color: var(--brand-dark);
  line-height: 1.2;
}

.pay-no {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 4px;
}

.pay-tag {
  margin-top: 12px;
}

.pay-actions {
  margin-top: 16px;
}

.pay-hint {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 8px;
}

.pay-hint.success {
  color: var(--success, #12a150);
}
</style>
