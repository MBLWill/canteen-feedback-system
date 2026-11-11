<template>
  <div class="bubble">
    <p v-for="(line, i) in lines" :key="i" class="bubble-line">{{ line }}</p>

    <div v-if="answer.items?.length" class="item-list">
      <div
        v-for="item in answer.items"
        :key="item.id + item.title"
        class="chat-item"
        :class="{ 'chat-item-clickable': item.link }"
        @click="item.link && go(item.link)"
      >
        <img v-if="item.image" class="chat-item-thumb" :src="item.image" :alt="item.title" loading="lazy">

        <div class="chat-item-main">
          <div class="chat-item-title">
            {{ item.title }}
            <span v-if="item.badge" class="badge" :class="'badge-' + item.badgeType">{{ item.badge }}</span>
          </div>
          <div class="chat-item-sub">{{ item.subtitle }}</div>
          <div class="chat-item-meta">{{ item.meta }}</div>
        </div>
        <button
          v-if="item.canAddToCart && item.id"
          class="chat-item-cart"
          :disabled="addingId === item.id"
          @click.stop="addCart(item)"
        >＋购物车</button>
        <span v-if="item.link" class="chat-item-go">
          {{ item.linkLabel || '查看' }}
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 6l6 6-6 6"/>
          </svg>
        </span>
      </div>
    </div>

    <div v-if="answer.actions?.length" class="action-row">
      <button
        v-for="action in answer.actions"
        :key="action.label + action.path"
        class="action-btn"
        :class="{ 'action-primary': action.primary }"
        @click="runAction(action)"
      >{{ action.label }}</button>
    </div>

    <div v-if="answer.sources?.length" class="sources">
      来源：{{ answer.sources.join(' ｜ ') }}
    </div>

    <div v-if="answer.quickReplies?.length" class="quick">
      <span
        v-for="q in answer.quickReplies"
        :key="q"
        class="quick-chip"
        @click="emit('ask', q)"
      >{{ q }}</span>
    </div>

    <div class="bubble-foot">
      <template v-if="rated === null">
        <span class="foot-label">这条回答有帮助吗</span>
        <el-button size="small" text @click="rate(1)">有帮助</el-button>
        <el-button size="small" text @click="rate(0)">没帮助</el-button>
      </template>
      <span v-else class="foot-done">已反馈，谢谢</span>
      <el-button
        v-if="answer.canFeedback"
        size="small"
        text
        type="warning"
        @click="go('/feedbacks')"
      >
        去留言反馈
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { rateChat } from '@/api/chat'
import { addToCart } from '@/api/cart'
import { useCartStore } from '@/store/cart'

const props = defineProps({
  answer: { type: Object, required: true }
})
const emit = defineEmits(['ask'])
const router = useRouter()
const cartStore = useCartStore()
const rated = ref(null)
const addingId = ref(null)

const lines = computed(() => String(props.answer.text || '').split('\n').filter(Boolean))

function go(path) {
  router.push(path)
}

function runAction(action) {
  if (!action?.path) return
  // 购物车是全局面板而非独立路由，/cart 直接打开购物车抽屉
  if (action.path === '/cart') {
    cartStore.openDrawer()
    return
  }
  router.push(action.path)
}

async function addCart(item) {
  if (addingId.value) return
  addingId.value = item.id
  try {
    await addToCart(item.id, 1)
    window.dispatchEvent(new CustomEvent('canteen:cart-changed'))
    ElMessage.success(`已加入购物车：${item.title}`)
    // 加购成功后直接打开购物车抽屉，让用户立刻看到已加购的商品
    cartStore.openDrawer()
  } catch {
    // 请求层已统一弹出错误提示，这里保持静默
  } finally {
    addingId.value = null
  }
}

async function rate(helpful) {
  if (!props.answer.logId) return
  try {
    await rateChat(props.answer.logId, helpful)
    rated.value = helpful
  } catch {
    rated.value = helpful
  }
}
</script>

<style scoped>
.bubble {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 12px 14px;
  box-shadow: var(--shadow-xs);
}
.bubble-line {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-1);
  white-space: pre-wrap;
}
.item-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
.chat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--brand-soft);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease;
}
.chat-item-clickable {
  cursor: pointer;
}
.chat-item-clickable:hover {
  border-color: var(--brand-line);
  background: #fff;
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}
.chat-item-go {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 500;
  color: var(--brand);
  padding: 5px 10px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--brand-line);
}
.chat-item-clickable:hover .chat-item-go {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.chat-item-cart {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--brand);
  background: #fff;
  border: 1px solid var(--brand-line);
  border-radius: 999px;
  padding: 5px 10px;
  cursor: pointer;
  transition: background .15s ease, color .15s ease;
}
.chat-item-cart:hover:not(:disabled) {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.chat-item-cart:disabled {
  opacity: .6;
  cursor: default;
}
.chat-item-thumb {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 10px;
  object-fit: cover;
  background: var(--border);
}
.chat-item-main {
  flex: 1;
  min-width: 0;
}
.chat-item-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 6px;
}
.chat-item-sub {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 2px;
}
.chat-item-meta {
  font-size: 12px;
  color: var(--brand-deep);
  margin-top: 4px;
}
.badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--border);
  color: var(--text-2);
}
.badge-success { background: #e7f7ec; color: #17803d; }
.badge-warning { background: #fdf1dc; color: #a55a00; }
.badge-danger  { background: #fdeaea; color: #b42318; }
.badge-info    { background: #e8f1fd; color: #175cd3; }
.sources {
  margin-top: 10px;
  font-size: 11px;
  color: var(--text-3);
}
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.action-btn {
  font-size: 12px;
  font-weight: 500;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--brand-line);
  background: #fff;
  color: var(--brand);
  cursor: pointer;
  transition: background .15s ease, color .15s ease;
}
.action-btn:hover {
  background: var(--brand-soft);
}
.action-primary {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}
.action-primary:hover {
  background: var(--brand-dark);
}
.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.quick-chip {
  font-size: 12px;
  padding: 4px 10px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--text-2);
  cursor: pointer;
}
.quick-chip:hover {
  border-color: var(--brand);
  color: var(--brand);
}
.bubble-foot {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--border);
}
.foot-label, .foot-done {
  font-size: 11px;
  color: var(--text-3);
  margin-right: auto;
}
</style>
