<template>
  <transition name="cart-bar">
    <div v-if="cartStore.count > 0" class="cart-bar">
      <button class="bar-info" @click="cartStore.openDrawer()">
        <span class="bar-icon" :class="{ bump: bumping }">
          <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="#fff" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="20" r="1.4" fill="#fff" stroke="none"/>
            <circle cx="18" cy="20" r="1.4" fill="#fff" stroke="none"/>
            <path d="M2.5 3h2.2l2.4 11.2a1.6 1.6 0 0 0 1.6 1.3h8.6a1.6 1.6 0 0 0 1.6-1.3L20.5 7H6"/>
          </svg>
          <i class="bar-count">{{ cartStore.count > 99 ? '99+' : cartStore.count }}</i>
        </span>
        <span class="bar-money">
          <b>¥{{ cartStore.totalAmount }}</b>
          <small>{{ cartStore.stallCount > 1 ? `${cartStore.stallCount} 个档口 · 分别结算` : '待取餐' }}</small>
        </span>
      </button>
      <button class="bar-go" @click="cartStore.openDrawer()">去结算</button>
    </div>
  </transition>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useCartStore } from '@/store/cart'

const cartStore = useCartStore()
const bumping = ref(false)
let bumpTimer = null

function bump() {
  bumping.value = false
  clearTimeout(bumpTimer)
  bumpTimer = setTimeout(() => {
    bumping.value = true
    bumpTimer = setTimeout(() => { bumping.value = false }, 320)
  }, 0)
}

function onChanged() {
  cartStore.refresh()
  bump()
}

function onOpen() {
  cartStore.openDrawer()
}

function onClose() {
  cartStore.closeDrawer()
}

onMounted(() => {
  window.addEventListener('canteen:cart-changed', onChanged)
  window.addEventListener('canteen:open-cart', onOpen)
  window.addEventListener('canteen:close-cart', onClose)
  cartStore.refresh()
})

onBeforeUnmount(() => {
  window.removeEventListener('canteen:cart-changed', onChanged)
  window.removeEventListener('canteen:open-cart', onOpen)
  window.removeEventListener('canteen:close-cart', onClose)
  clearTimeout(bumpTimer)
})
</script>

<style scoped>
.cart-bar {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 900;
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 6px 0 8px;
  border-radius: 28px;
  background: #2c2c2a;
  box-shadow: 0 8px 24px rgba(0, 0, 0, .22), 0 2px 6px rgba(0, 0, 0, .12);
  color: #fff;
}

.bar-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px 0 8px;
  height: 100%;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.bar-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  transition: transform .32s cubic-bezier(.34, 1.56, .64, 1);
}

.bar-icon.bump {
  transform: scale(1.22);
}

.bar-count {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 9px;
  background: var(--brand);
  color: #fff;
  font-size: 11px;
  font-style: normal;
  line-height: 17px;
  text-align: center;
}

.bar-money {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.bar-money b {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: .2px;
}

.bar-money small {
  font-size: 11px;
  color: rgba(255, 255, 255, .55);
}

.bar-go {
  height: 44px;
  padding: 0 20px;
  margin-left: 4px;
  border: none;
  border-radius: 22px;
  background: var(--brand);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: filter .18s ease;
}

.bar-go:hover {
  filter: brightness(1.06);
}

.cart-bar-enter-active,
.cart-bar-leave-active {
  transition: transform .26s cubic-bezier(.34, 1.3, .64, 1), opacity .2s ease;
}

.cart-bar-enter-from,
.cart-bar-leave-to {
  transform: translateY(16px) scale(.96);
  opacity: 0;
}
</style>
