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
      <div class="pay-amount">¥{{ session?.amount ?? '0.00' }}</div>
      <div class="pay-no">支付单号 {{ session?.payNo }}</div>
      <el-tag size="small" :type="tagType" class="pay-tag">{{ statusLabel }}</el-tag>

      <div v-if="pay.status === 'CREATED' && session?.channel === 'mock'" class="pay-actions">
        <el-button type="success" size="small" :loading="pay.mocking" @click="handleMock">模拟支付成功</el-button>
      </div>
      <div v-if="pay.status === 'CREATED' && session?.payUrl" class="pay-actions">
        <el-button type="primary" size="small" @click="openCashier">打开收银台</el-button>
        <div class="pay-hint">付款后回到本页，系统会自动确认结果</div>
      </div>
      <div v-if="pay.status === 'PAID'" class="pay-hint success">支付已完成，可在「我的订单」查看取餐时间</div>
    </div>

    <template #footer>
      <el-button size="small" @click="pay.hide()">关闭</el-button>
      <el-button v-if="pay.status === 'PAID'" type="success" size="small" @click="goOrders">查看我的订单</el-button>
      <el-button v-else type="primary" size="small" :loading="pay.querying" @click="pay.query()">查询支付结果</el-button>
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
    if (!value) pay.hide()
  }
})

const session = computed(() => pay.session)

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

function goOrders() {
  pay.hide()
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
