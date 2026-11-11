<template>
  <teleport to="body">
    <transition name="mask">
      <div v-if="cartStore.drawerOpen" class="cart-mask" @click="close" />
    </transition>

    <transition name="drawer">
      <aside v-if="cartStore.drawerOpen" class="cart-drawer" role="dialog" aria-label="购物车">
        <header class="dr-head">
          <div class="head-main">
            <template v-if="step === 'list'">
              <h3>我的购物车</h3>
              <p>{{ cart.totalQuantity || 0 }} 件 · 合计 ¥{{ cart.totalAmount ?? '0.00' }}</p>
            </template>
            <template v-else>
              <button class="dr-back" @click="step = 'list'">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M15 18l-6-6 6-6"/>
                </svg>
                返回购物车
              </button>
              <h3>确认订单</h3>
            </template>
          </div>
          <button class="dr-close" title="关闭" @click="close">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </header>

        <div v-if="step === 'list'" class="dr-body">
          <div v-if="!allItems.length" class="dr-empty">
            <div class="empty-ring">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="20" r="1.4" fill="currentColor" stroke="none"/>
                <circle cx="18" cy="20" r="1.4" fill="currentColor" stroke="none"/>
                <path d="M2.5 3h2.2l2.4 11.2a1.6 1.6 0 0 0 1.6 1.3h8.6a1.6 1.6 0 0 0 1.6-1.3L20.5 7H6"/>
              </svg>
            </div>
            <p>购物车还空着</p>
            <button class="empty-go" @click="goDishes">去挑几个菜</button>
          </div>

          <template v-else>
            <div class="stall-card">
              <div class="stall-head">
                <span class="stall-name">全部菜品</span>
                <span class="stall-window">{{ allItems.length }} 种 · 来自 {{ groups.length }} 个档口</span>
                <button class="stall-clear" @click="clearAll">清空购物车</button>
              </div>

              <template v-for="g in groups" :key="g.stallId">
                <div class="stall-divider">
                  <span class="stall-divider-name">{{ g.stallName }}</span>
                  <span class="stall-divider-window">{{ g.windowNo }} 号窗口</span>
                  <span class="stall-divider-count">{{ g.totalQuantity }} 件</span>
                </div>
                <div
                  v-for="item in g.items"
                  :key="item.id"
                  class="row"
                  :class="{ off: item.dishStatus !== 1 }"
                >
                  <label class="check" @click.prevent="toggleItem(item)">
                    <span class="box" :class="{ checked: isChecked(item) }">
                      <svg v-if="isChecked(item)" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 6L9 17l-5-5"/>
                      </svg>
                    </span>
                  </label>

                  <div class="thumb" :style="{ background: coverGradient(item.category) }">
                    <img v-if="item.image && !broken[item.id]" :src="item.image" alt="" @error="broken[item.id] = true">
                    <span v-else>{{ (item.dishName || '菜').slice(0, 1) }}</span>
                  </div>

                  <div class="info">
                    <div class="name">
                      {{ item.dishName }}
                      <em v-if="item.dishStatus !== 1">已下架</em>
                    </div>
                    <div class="unit">¥{{ item.price }}<small> / 份</small></div>
                  </div>

                  <div class="stepper">
                    <button :disabled="displayQty(item) <= 1" @click="changeQty(item, displayQty(item) - 1)">
                      <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><path d="M5 12h14"/></svg>
                    </button>
                    <span>{{ displayQty(item) }}</span>
                    <button :disabled="displayQty(item) >= maxQty" @click="changeQty(item, displayQty(item) + 1)">
                      <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
                    </button>
                  </div>

                  <button class="del" title="移除" @click="removeItem(item)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13"/>
                    </svg>
                  </button>
                </div>
              </template>
            </div>
          </template>
        </div>

        <div v-else class="dr-body confirm">
          <div class="cf-block">
            <div class="cf-label">取餐时间</div>
            <el-date-picker
              v-model="form.pickupTime"
              type="datetime"
              placeholder="选择取餐时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disabledDate"
              class="cf-picker"
            />
          </div>

          <div class="cf-block">
            <div class="cf-label">备注 <small>口味、忌口、打包等</small></div>
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="选填"
            />
          </div>

          <div class="cf-block">
            <div class="cf-label">已选菜品 · {{ checkedItems.length }} 种 · {{ groups.length }} 个档口</div>
            <div class="cf-list">
              <div v-for="item in checkedItems" :key="item.id" class="cf-row">
                <span class="cf-name">{{ item.dishName }}<i class="cf-stall">{{ item.stallName }}</i></span>
                <span class="cf-qty">×{{ displayQty(item) }}</span>
                <span class="cf-amt">¥{{ lineAmount(item) }}</span>
              </div>
            </div>
            <p class="cf-tip">不同档口将自动拆单分别支付；打包费、配送费按下单结果计算</p>
          </div>
        </div>

        <footer v-if="groups.length" class="dr-foot">
          <template v-if="step === 'list'">
            <label class="check foot-check" @click.prevent="toggleAll">
              <span class="box" :class="{ checked: allChecked, half: !allChecked && checkedItems.length > 0 }">
                <svg v-if="allChecked" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 6L9 17l-5-5"/>
                </svg>
                <i v-else-if="checkedItems.length" class="half-bar" />
              </span>
              全选
            </label>
            <div class="foot-sum">
              <span>已选 <b>{{ checkedQuantity }}</b> 件</span>
              <strong>¥{{ checkedAmount }}</strong>
            </div>
            <button class="foot-go" :disabled="!checkedQuantity" @click="toConfirm">去结算</button>
          </template>
          <template v-else>
            <div class="foot-sum">
              <span>{{ checkedQuantity }} 件</span>
              <strong>¥{{ checkedAmount }}</strong>
            </div>
            <button class="foot-go" :disabled="submitting" @click="submit">
              {{ submitting ? '提交中…' : '提交并支付' }}
            </button>
          </template>
        </footer>
      </aside>
    </transition>
  </teleport>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkoutCart, clearCart, getCart, removeCartItem, updateCartQuantity } from '@/api/cart'
import { useCartStore } from '@/store/cart'
import { usePayStore } from '@/store/pay'
import { coverGradient } from '@/utils/dishImage'

const router = useRouter()
const cartStore = useCartStore()
const payStore = usePayStore()

const cart = ref({ groups: [], totalQuantity: 0, totalAmount: '0.00', maxQuantityPerDish: 10 })
const groups = computed(() => cart.value.groups || [])
const maxQty = computed(() => cart.value.maxQuantityPerDish || 10)

const step = ref('list')
const submitting = ref(false)
const form = reactive({ pickupTime: '', remark: '' })

/** 用户主动取消勾选的条目 id，其余默认选中（加购新品即自动进入结算范围） */
const deselected = reactive({})
/** 数量本地覆盖，点一下立刻响应，不等待接口 */
const localQty = reactive({})
const broken = reactive({})

/** 所有档口的菜品合并成一个平铺列表，不分档口展示 */
const allItems = computed(() =>
  groups.value.flatMap(g => (g.items || []).map(i => ({ ...i })))
)
/** 在售菜品（可勾选） */
const salableItems = computed(() => allItems.value.filter(i => i.dishStatus === 1))

const checkedItems = computed(() => salableItems.value.filter(i => isChecked(i)))
const allChecked = computed(() => salableItems.value.length > 0 && salableItems.value.every(i => isChecked(i)))
const checkedQuantity = computed(() => checkedItems.value.reduce((sum, i) => sum + displayQty(i), 0))
const checkedAmount = computed(() =>
  checkedItems.value.reduce((sum, i) => sum + Number(i.price) * displayQty(i), 0).toFixed(2)
)

function isChecked(item) {
  return !deselected[item.id]
}

function displayQty(item) {
  return localQty[item.id] ?? item.quantity
}

function lineAmount(item) {
  return (Number(item.price) * displayQty(item)).toFixed(2)
}

function toggleItem(item) {
  if (item.dishStatus !== 1) return
  if (deselected[item.id]) delete deselected[item.id]
  else deselected[item.id] = true
}

function toggleAll() {
  const target = !allChecked.value
  for (const item of salableItems.value) {
    if (target) delete deselected[item.id]
    else deselected[item.id] = true
  }
}

async function loadCart() {
  const res = await getCart()
  cart.value = res.data || { groups: [] }
  cartStore.count = cart.value.totalQuantity || 0
  cartStore.totalAmount = cart.value.totalAmount ?? '0.00'
  cartStore.stallCount = groups.value.length
  if (!groups.value.length) {
    step.value = 'list'
  }
}

async function changeQty(item, qty) {
  if (qty < 1 || qty > maxQty.value) return
  localQty[item.id] = qty
  try {
    await updateCartQuantity(item.id, qty)
    await loadCart()
    delete localQty[item.id]
  } catch {
    delete localQty[item.id]
  }
}

function removeItem(item) {
  ElMessageBox.confirm(`把「${item.dishName}」从购物车移除？`, '提示', { type: 'warning' })
    .then(async () => {
      await removeCartItem(item.id)
      delete deselected[item.id]
      delete localQty[item.id]
      await loadCart()
      cartStore.refresh()
    })
    .catch(() => {})
}

function clearAll() {
  const total = groups.value.reduce((sum, g) => sum + (g.totalQuantity || 0), 0)
  ElMessageBox.confirm(`清空购物车的 ${total} 件商品？`, '提示', { type: 'warning' })
    .then(async () => {
      await clearCart()
      deselected && Object.keys(deselected).forEach(k => delete deselected[k])
      await loadCart()
      cartStore.refresh()
      ElMessage.success('已清空购物车')
    })
    .catch(() => {})
}

function toConfirm() {
  if (!checkedQuantity.value) {
    ElMessage.warning('请先勾选要结算的菜品')
    return
  }
  form.pickupTime = form.pickupTime || defaultPickupTime()
  step.value = 'confirm'
}

function defaultPickupTime() {
  const d = new Date(Date.now() + 30 * 60000)
  d.setSeconds(0, 0)
  d.setMinutes(Math.ceil(d.getMinutes() / 5) * 5)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:00`
}

function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const limit = new Date(today)
  limit.setDate(limit.getDate() + 7)
  return date.getTime() < today.getTime() || date.getTime() > limit.getTime()
}

async function submit() {
  if (!form.pickupTime) {
    ElMessage.warning('请选择取餐时间')
    return
  }
  submitting.value = true
  try {
    const res = await checkoutCart({
      pickupTime: form.pickupTime,
      remark: form.remark || undefined,
      itemIds: checkedItems.value.map(i => i.id)
    })
    // 后端跨档口会自动拆成多笔订单+支付单，返回数组
    const list = Array.isArray(res.data) ? res.data : [res.data]
    step.value = 'list'
    form.remark = ''
    form.pickupTime = ''
    for (const key of Object.keys(localQty)) delete localQty[key]
    await loadCart()
    cartStore.refresh()

    // 打开支付面板，按笔依次支付
    payStore.openBatch(list)
    cartStore.closeDrawer()
  } finally {
    submitting.value = false
  }
}

function close() {
  cartStore.closeDrawer()
}

function goDishes() {
  cartStore.closeDrawer()
  router.push('/dishes')
}

const openDrawer = () => cartStore.openDrawer()
const closeDrawer = () => cartStore.closeDrawer()
const onCartChanged = () => {
  cartStore.refresh()
  // 抽屉已经打开时，也要重新拉取列表，否则 AI 加购后抽屉里看不到新加的商品
  if (cartStore.drawerOpen) loadCart()
}

watch(() => cartStore.drawerOpen, value => {
  if (value) loadCart()
})

onMounted(() => {
  window.addEventListener('canteen:open-cart', openDrawer)
  window.addEventListener('canteen:close-cart', closeDrawer)
  window.addEventListener('canteen:cart-changed', onCartChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('canteen:open-cart', openDrawer)
  window.removeEventListener('canteen:close-cart', closeDrawer)
  window.removeEventListener('canteen:cart-changed', onCartChanged)
})
</script>

<style scoped>
.cart-mask {
  position: fixed;
  inset: 0;
  z-index: 1900;
  background: rgba(24, 18, 12, .38);
  backdrop-filter: blur(1px);
}

.cart-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 1901;
  width: min(480px, 100vw);
  display: flex;
  flex-direction: column;
  background: #f7f4f0;
  box-shadow: -12px 0 40px rgba(0, 0, 0, .16);
}

.dr-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 14px;
  background: #fff;
  border-bottom: 1px solid var(--border);
}

.dr-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
}

.dr-head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-3);
}

.dr-back {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 4px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-2);
  font-size: 12px;
  cursor: pointer;
}

.dr-back:hover { color: var(--brand); }

.dr-close {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
}

.dr-close:hover { background: #f0ece6; color: var(--text-1); }

.dr-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 20px 20px;
}

.dr-empty {
  padding: 64px 0 40px;
  text-align: center;
  color: var(--text-3);
}

.empty-ring {
  width: 62px;
  height: 62px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #ece7df;
  color: #b8b0a4;
}

.dr-empty p {
  margin: 0 0 16px;
  font-size: 13px;
}

.empty-go {
  padding: 9px 22px;
  border: none;
  border-radius: 18px;
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.stall-pills {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.pill {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  transition: all .16s;
}

.pill i {
  font-style: normal;
  font-size: 11px;
  color: var(--text-3);
}

.pill.active {
  border-color: var(--brand);
  background: #fff3e8;
  color: var(--brand-dark);
}

.pill.active i { color: var(--brand); }

.stall-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border);
}

.stall-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
}

.stall-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
}

.stall-window {
  font-size: 11px;
  color: var(--text-3);
}

.stall-clear {
  margin-left: auto;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-3);
  font-size: 12px;
  cursor: pointer;
}

.stall-clear:hover { color: #b42318; }

.stall-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px 6px;
  background: #faf6f0;
  border-bottom: 1px solid #f3eee7;
}

.stall-divider-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-1);
}

.stall-divider-window {
  font-size: 11px;
  color: var(--text-3);
}

.stall-divider-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-3);
}

.row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #f3eee7;
}

.row:last-child { border-bottom: none; }

.row.off { opacity: .55; }

.check {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-2);
}

.box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 1.5px solid #cfc6b8;
  border-radius: 50%;
  transition: all .16s;
}

.box.checked {
  border-color: var(--brand);
  background: var(--brand);
}

.box.half { border-color: var(--brand); }

.half-bar {
  width: 9px;
  height: 2.5px;
  border-radius: 2px;
  background: var(--brand);
}

.thumb {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 9px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.name em {
  margin-left: 6px;
  padding: 1px 5px;
  border-radius: 4px;
  background: #f0ece6;
  color: var(--text-3);
  font-size: 10px;
  font-style: normal;
  font-weight: 400;
}

.unit {
  margin-top: 3px;
  font-size: 13px;
  font-weight: 600;
  color: #c2410c;
}

.unit small {
  font-weight: 400;
  font-size: 11px;
  color: var(--text-3);
}

.stepper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 26px;
  border: 1px solid var(--border);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.stepper button {
  width: 24px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
}

.stepper button:hover:not(:disabled) { background: #f7f2ec; color: var(--brand); }
.stepper button:disabled { color: #d6cec2; cursor: not-allowed; }

.stepper span {
  min-width: 26px;
  text-align: center;
  font-size: 13px;
  color: var(--text-1);
  border-left: 1px solid var(--border);
  border-right: 1px solid var(--border);
  line-height: 24px;
}

.del {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #c3bab0;
  cursor: pointer;
  opacity: 0;
  transition: all .16s;
}

.row:hover .del { opacity: 1; }
.del:hover { background: #fdeaea; color: #b42318; }

.multi-tip {
  margin: 12px 0 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-3);
}

.dr-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid var(--border);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, .04);
}

.foot-check { flex-shrink: 0; }

.foot-sum {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

.foot-sum span {
  font-size: 11px;
  color: var(--text-3);
}

.foot-sum strong {
  font-size: 19px;
  font-weight: 700;
  color: #c2410c;
}

.foot-go {
  flex-shrink: 0;
  height: 42px;
  padding: 0 26px;
  border: none;
  border-radius: 21px;
  background: var(--brand);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.foot-go:disabled {
  background: #e2dbd1;
  color: #a89f93;
  cursor: not-allowed;
}

.confirm .cf-block {
  margin-bottom: 14px;
  padding: 14px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 14px;
}

.cf-label {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
}

.cf-label small {
  font-weight: 400;
  font-size: 11px;
  color: var(--text-3);
  margin-left: 4px;
}

.cf-picker { width: 100%; }

.cf-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cf-row {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: var(--text-2);
}

.cf-name {
  flex: 1;
  min-width: 0;
}

.cf-name .cf-stall {
  margin-left: 6px;
  font-size: 11px;
  font-style: normal;
  color: var(--text-3);
}

.cf-qty {
  margin-left: auto;
  margin-right: 14px;
  font-size: 12px;
  color: var(--text-3);
}

.cf-amt {
  min-width: 56px;
  text-align: right;
  color: var(--text-1);
}

.cf-tip {
  margin: 10px 0 0;
  font-size: 11px;
  color: var(--text-3);
}

.mask-enter-active,
.mask-leave-active { transition: opacity .22s ease; }
.mask-enter-from,
.mask-leave-to { opacity: 0; }

.drawer-enter-active,
.drawer-leave-active { transition: transform .28s cubic-bezier(.32, .72, .32, 1); }
.drawer-enter-from,
.drawer-leave-to { transform: translateX(100%); }

@media (max-width: 640px) {
  .cart-drawer { width: 100vw; }
  .dr-body { padding: 12px 14px 16px; }
  .dr-head, .dr-foot { padding-left: 14px; padding-right: 14px; }
}
</style>
