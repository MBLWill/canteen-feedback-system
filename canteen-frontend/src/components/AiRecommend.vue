<template>
  <div class="ai-rec">
    <!-- 头部：AI 小助手 + 打招呼 + 机器人形象 -->
    <div class="ai-rec-head">
      <div class="ai-rec-hd-left">
        <div class="ai-rec-title">
          <span class="ai-rec-bar"></span>
          <span class="ai-rec-title-text">AI 小助手</span>
        </div>
        <div class="ai-rec-sub">有任何问题都可以问哦～</div>
      </div>
      <img src="/ui/assistant/happy.png" class="ai-rec-mascot" alt="AI 小助手">
    </div>

    <!-- 输入框 + 发送按钮：默认文字仅为占位提示，点击箭头才跳转食堂小助手 -->
    <div class="ai-rec-prompt">
      <input
        v-model="question"
        class="ai-rec-input"
        type="text"
        placeholder="推荐一下今天的热销菜吧！"
        @keyup.enter="generate"
      />
      <button class="ai-rec-send" title="发送" @click="generate">
        <el-icon><ArrowRight /></el-icon>
      </button>
    </div>

    <!-- 推荐结果 -->
    <div class="ai-rec-result">
      <div class="ai-rec-result-head">
        <span class="ai-rec-result-title">
          <span class="ai-rec-bar"></span>热门推荐
        </span>
        <span class="ai-rec-refresh" @click="shuffle">
          换一批
          <el-icon><Refresh /></el-icon>
        </span>
      </div>

      <div v-if="thinking" class="ai-rec-loading">AI 小助手正在为你挑选…</div>
      <div v-else-if="picks.length" class="ai-rec-dishes">
        <div v-for="d in picks" :key="'ai-' + d.dishId" class="ai-rec-dish" @click="go(d)">
          <div class="ai-rec-img" :style="{ background: grad(d.category) }">
            <img v-if="d.image" :src="d.image" :alt="d.dishName" loading="lazy" @error="$event.target.style.display='none'">
            <span v-else class="ai-rec-initial">{{ (d.dishName || '菜').slice(0, 1) }}</span>
          </div>
          <div class="ai-rec-name" :title="d.dishName">{{ d.dishName }}</div>
          <div class="ai-rec-price">¥{{ price(d.price) }}</div>
        </div>
      </div>
      <div v-else class="ai-rec-empty">AI 小助手正在盘点今日热销菜…</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { coverGradient } from '@/utils/dishImage'

const props = defineProps({
  dishes: { type: Array, default: () => [] }
})

const router = useRouter()
const offset = ref(0)
const thinking = ref(false)
const question = ref('')

const picks = computed(() => {
  const list = props.dishes || []
  if (!list.length) return []
  const n = list.length
  return Array.from({ length: Math.min(3, n) }, (_, i) => list[(offset.value + i) % n])
})

function shuffle() {
  const n = (props.dishes || []).length
  if (n > 0) offset.value = (offset.value + 3) % n
}

// 「发送」把问题发给 AI 小助手：唤醒全局小助手窗口并自动发送该问题
// 用户输入的内容优先，未输入则回退到默认提问
function generate() {
  const q = question.value.trim() || '推荐一下今天的热销菜吧！'
  window.dispatchEvent(new CustomEvent('canteen:assistant-ask', { detail: q }))
}

function go(d) {
  // 点菜品先跳对应档口页并高亮，再点菜品才进详情（与美食广场主网格一致）
  if (!d || !d.dishId) return
  if (d.stallId) {
    router.push(`/stalls/${d.stallId}?highlight=${d.dishId}`)
  } else {
    router.push(`/dishes/${d.dishId}`)
  }
}

function grad(cat) {
  return coverGradient(cat)
}

function price(p) {
  if (p == null) return '—'
  return Number(p).toFixed(2)
}
</script>

<style scoped>
.ai-rec {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  width: 100%;
  box-sizing: border-box;
}

.ai-rec-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  position: relative;
}

.ai-rec-hd-left {
  min-width: 0;
}

.ai-rec-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-rec-bar {
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ff9a3d, var(--brand));
  display: inline-block;
}

.ai-rec-title-text {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-1);
}

.ai-rec-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3);
}

.ai-rec-mascot {
  width: 40px;
  height: 40px;
  object-fit: contain;
  flex-shrink: 0;
  filter: drop-shadow(0 4px 10px rgba(255, 122, 26, 0.28));
}

.ai-rec-prompt {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.ai-rec-input {
  flex: 1;
  min-width: 0;
  padding: 8px 12px;
  border-radius: 14px;
  background: #f5f6f8;
  border: 1px solid var(--border);
  color: var(--text-1);
  font-size: 12px;
  line-height: 1.4;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s ease;
}

.ai-rec-input::placeholder {
  color: var(--text-3);
}

.ai-rec-input:focus {
  border-color: var(--brand-line);
  background: #fff;
}

.ai-rec-send {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #ff9a3d, var(--brand));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 3px 8px rgba(255, 122, 26, 0.32);
  transition: transform 0.15s ease;
}

.ai-rec-send:hover {
  transform: scale(1.08);
}

.ai-rec-result {
  margin-top: 10px;
}

.ai-rec-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.ai-rec-result-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
}

.ai-rec-refresh {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.15s ease;
}

.ai-rec-refresh:hover {
  color: var(--brand);
}

.ai-rec-dishes {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.ai-rec-dish {
  cursor: pointer;
  text-align: center;
  transition: transform 0.15s ease;
}

.ai-rec-dish:hover {
  transform: translateY(-2px);
}

.ai-rec-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.ai-rec-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  inset: 0;
}

.ai-rec-initial {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.ai-rec-name {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-rec-price {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 700;
  color: var(--brand);
}

.ai-rec-empty {
  font-size: 12px;
  color: var(--text-3);
  text-align: center;
  padding: 14px 0;
}

.ai-rec-loading {
  font-size: 12px;
  color: var(--text-3);
  text-align: center;
  padding: 22px 0;
  animation: ai-pulse 1.2s ease-in-out infinite;
}

@keyframes ai-pulse {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}
</style>