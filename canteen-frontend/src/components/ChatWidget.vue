<template>
  <div class="chat-widget">
    <transition name="chat-pop">
      <div v-if="open" class="chat-panel" :style="panelStyle">
        <div class="chat-head">
          <div class="head-avatar">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" fill="#fff" stroke="none"/>
            </svg>
          </div>
          <div class="head-text">
            <div class="head-title">食堂小助手</div>
            <div class="head-sub">{{ engineLabel }}</div>
          </div>
        </div>

        <div ref="bodyEl" class="chat-body">
          <div v-if="!messages.length" class="chat-welcome">
            <div class="welcome-icon">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--brand)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
              </svg>
            </div>
            <p class="welcome-title">你好，想问点什么？</p>
            <p class="welcome-sub">可以直接输入菜名，比如「宫保鸡丁」</p>
            <div class="welcome-chips">
              <span v-for="s in suggestions" :key="s" class="quick-chip" @click="send(s)">{{ s }}</span>
            </div>
          </div>

          <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
            <template v-if="m.role === 'user'">
              <span class="user-text">{{ m.text }}</span>
            </template>
            <template v-else>
              <!-- 流式中：先显示已到达的文字 + 光标，等 done 再渲染卡片 -->
              <div v-if="m.streaming" class="bubble streaming-bubble">
                <p class="streaming-text">{{ m.answer.text }}<span class="caret"></span></p>
              </div>
              <AnswerBubble v-else :answer="m.answer" @ask="send" />
            </template>
          </div>

          <div v-if="loading" class="msg bot">
            <span class="typing"><i></i><i></i><i></i></span>
            <span class="typing-hint">{{ thinkingText }}</span>
          </div>
        </div>

        <div class="chat-input">
          <input
            v-model="draft"
            class="chat-input-field"
            placeholder="输入菜名或问题…"
            maxlength="200"
            @keyup.enter="send(draft)"
          >
          <button class="send-btn" :disabled="loading" @click="send(draft)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>

    <button
      class="chat-fab"
      :class="{ 'fab-active': open, 'fab-dragging': isDragging }"
      :style="fabStyle"
      :title="open ? '关闭小助手' : '食堂小助手（可拖动）'"
      @pointerdown="onFabDown"
    >
      <svg v-if="!open" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
        <path d="M8 10h8M8 14h5"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" width="22" height="22" stroke="#fff" stroke-width="2.4" stroke-linecap="round">
        <path d="M6 6l12 12M18 6L6 18"/>
      </svg>
      <span v-if="!open" class="fab-pulse"></span>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import AnswerBubble from '@/components/chat/AnswerBubble.vue'
import { askChat, askChatStream, getChatEngine, getChatSuggestions } from '@/api/chat'

const FAB = 58
/**
 * 面板尺寸：打开即大窗口。
 * 宽度上限 560px，高度撑满视口减去上下边距。
 */
const PANEL_W = 560
const PANEL_MIN_W = 420
const EDGE = 16
/** 圆球直径 */
const FAB_SIZE = 58

const open = ref(false)
const draft = ref('')
const loading = ref(false)
const messages = ref([])
const suggestions = ref([])
const bodyEl = ref(null)
/** 当前推理引擎，仅用于头部文案 */
const engine = ref('agent')
const engineLabel = computed(() => (engine.value === 'agent'
  ? '智能助手 · 实时查库，随问随答'
  : '本地助手 · 菜品 · 档口 · 规则'))

const pos = reactive({ x: 0, y: 0 })
const vp = reactive({ w: 1280, h: 800 })
const isDragging = ref(false)
let dragState = null

/**
 * 等待时的趣味提示语。
 * 不暴露「正在查询数据库」这类实现细节，用拟人化的说法更亲切。
 */
const THINKING_WORDS = [
  '让我想想…',
  '正在翻食堂的小本本…',
  '努力查询中…',
  '琢磨一下你的问题…',
  '去后厨打听打听…',
  '马上就好…',
  '认真思考中…'
]
const thinkingText = ref(THINKING_WORDS[0])
let thinkingTimer = null

function startThinking() {
  stopThinking()
  thinkingText.value = THINKING_WORDS[Math.floor(Math.random() * THINKING_WORDS.length)]
  let i = THINKING_WORDS.indexOf(thinkingText.value)
  thinkingTimer = setInterval(() => {
    i = (i + 1) % THINKING_WORDS.length
    thinkingText.value = THINKING_WORDS[i]
  }, 2200)
}

function stopThinking() {
  if (thinkingTimer) {
    clearInterval(thinkingTimer)
    thinkingTimer = null
  }
}

function clampPos() {
  pos.x = Math.min(Math.max(pos.x, 12), vp.w - FAB - 12)
  pos.y = Math.min(Math.max(pos.y, 12), vp.h - FAB - 12)
}

function onResize() {
  vp.w = window.innerWidth
  vp.h = window.innerHeight
  clampPos()
}

onMounted(async () => {
  vp.w = window.innerWidth
  vp.h = window.innerHeight
  // 默认停在右下角，避开悬浮购物车条（高度约 80px）
  pos.x = vp.w - FAB - 28
  pos.y = vp.h - FAB - 92
  window.addEventListener('resize', onResize)
  try {
    const res = await getChatSuggestions()
    suggestions.value = res.data || []
  } catch {
    suggestions.value = ['今天有什么推荐', '怎么投诉某个窗口', '食堂几点营业']
  }
  try {
    const res = await getChatEngine()
    engine.value = res.data || 'agent'
  } catch {
    engine.value = 'agent'
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  stopThinking()
})

function onFabDown(e) {
  if (e.button !== undefined && e.button !== 0) return
  e.preventDefault()
  // 展开时圆球是关闭按钮，固定在面板右下角，不参与拖拽
  if (open.value) {
    dragState = { sx: e.clientX, sy: e.clientY, ox: pos.x, oy: pos.y, moved: false, fixed: true }
    window.addEventListener('pointerup', onFabUp)
    return
  }
  dragState = { sx: e.clientX, sy: e.clientY, ox: pos.x, oy: pos.y, moved: false, fixed: false }
  window.addEventListener('pointermove', onFabMove)
  window.addEventListener('pointerup', onFabUp)
}

function onFabMove(e) {
  if (!dragState) return
  const dx = e.clientX - dragState.sx
  const dy = e.clientY - dragState.sy
  if (!dragState.moved && Math.hypot(dx, dy) > 5) {
    dragState.moved = true
    isDragging.value = true
  }
  if (dragState.moved) {
    pos.x = dragState.ox + dx
    pos.y = dragState.oy + dy
    clampPos()
  }
}

function onFabUp() {
  window.removeEventListener('pointermove', onFabMove)
  window.removeEventListener('pointerup', onFabUp)
  const state = dragState
  dragState = null
  if (!state) return

  // 展开态：圆球就是关闭按钮，点一下收起
  if (state.fixed) {
    open.value = false
    return
  }

  if (state.moved) {
    setTimeout(() => { isDragging.value = false }, 50)
  } else {
    open.value = !open.value
  }
}

/**
 * 面板跟随圆球位置浮动。
 * <p>
 * 圆球收起时停在哪，展开后面板就以该点为锚点向左上展开；
 * 圆球完全落在面板右下方外侧，中间留一道缝隙。
 */
const PANEL_H_MIN = 420
/** 圆球与面板之间的缝隙 */
const FAB_GAP = 12

const panelSize = computed(() => {
  const width = Math.max(PANEL_MIN_W, Math.min(PANEL_W, vp.w - EDGE * 2))
  const centerY = pos.y + FAB_SIZE / 2
  // 可用高度 = 球心到视口顶部，再扣掉球体半径 + 缝隙（球在面板下方）
  const availH = centerY - FAB_SIZE / 2 - FAB_GAP - EDGE
  const height = Math.max(PANEL_H_MIN, Math.min(vp.h - EDGE * 2, availH))
  return { width, height }
})

const panelStyle = computed(() => {
  const { width, height } = panelSize.value
  const centerX = pos.x + FAB_SIZE / 2
  const centerY = pos.y + FAB_SIZE / 2
  // 面板右下角对齐球心，但要让出球体+缝隙的宽度，保证球完全落在面板外且不出屏
  let left = centerX - width - FAB_SIZE / 2 - FAB_GAP
  let top = centerY - height - FAB_SIZE / 2 - FAB_GAP
  // 只做最小边界保护，保持「跟随圆球」的语义
  left = Math.max(EDGE, Math.min(left, vp.w - width - EDGE))
  top = Math.max(EDGE, top)
  return {
    left: left + 'px',
    top: top + 'px',
    width: width + 'px',
    height: height + 'px'
  }
})

/**
 * 展开时圆球停靠的实际位置：面板右下角外侧，与面板留一道缝隙。
 * 收起时用 pos（用户拖动后的位置）。
 */
const fabStyle = computed(() => {
  if (!open.value) {
    return { left: pos.x + 'px', top: pos.y + 'px' }
  }
  const { width, height } = panelSize.value
  const p = panelStyle.value
  const left = parseFloat(p.left)
  const top = parseFloat(p.top)
  // 球体完全在面板外，与面板右下角之间留 GAP 的缝：
  // 球左上角 = 面板右下角 + GAP
  const GAP = 12
  return {
    left: (left + width + GAP) + 'px',
    top: (top + height + GAP) + 'px'
  }
})

async function send(text) {
  const question = String(text || '').trim()
  if (!question || loading.value) return
  draft.value = ''
  messages.value.push({ role: 'user', text: question })
  loading.value = true
  startThinking()
  scrollBottom()

  // 先插入一条空的机器人消息，流式过程中往里追加文字
  const botMsg = reactive({
    role: 'bot',
    streaming: false,
    answer: { text: '', hit: true, items: [], quickReplies: [], sources: null }
  })
  messages.value.push(botMsg)
  let streaming = false

  try {
    await askChatStream(question, buildHistory(), {
      onDelta(piece) {
        if (!streaming) {
          streaming = true
          botMsg.streaming = true
          loading.value = false
          stopThinking()
        }
        botMsg.answer.text += piece
        scrollBottom()
      },
      onDone(answer) {
        if (answer) {
          // 用服务端最终答案覆盖，确保文字与卡片一致
          botMsg.answer = {
            ...answer,
            text: answer.text || botMsg.answer.text
          }
        }
        botMsg.streaming = false
        scrollBottom()
      }
    })
  } catch (e) {
    botMsg.streaming = false
    if (botMsg.answer.text) {
      // 已经流出一部分，保留内容只提示中断
      botMsg.answer.text += '\n（回答中断，请重试）'
    } else {
      // 一个字都没收到：退回非流式接口兜底
      try {
        const res = await askChat(question, buildHistory())
        botMsg.answer = res.data
      } catch (e2) {
        botMsg.answer = {
          text: e2?.message || e?.message || '暂时没能回答，稍后再试试',
          hit: false,
          items: [],
          quickReplies: []
        }
      }
    }
  } finally {
    stopThinking()
    loading.value = false
    botMsg.streaming = false
    if (!botMsg.answer.text) {
      botMsg.answer.text = '暂时没能回答，稍后再试试'
    }
    scrollBottom()
  }
}

/**
 * 组装多轮历史：只保留最近 6 轮（12 条），供后端理解「它」「那家」这类指代。
 * 本轮提问已经单独传，历史里只放之前的对话。
 */
function buildHistory() {
  const list = []
  const recent = messages.value.slice(-12)
  for (const m of recent) {
    if (m.role === 'user' && m.text) {
      list.push({ role: 'user', content: m.text })
    } else if (m.role === 'bot' && m.answer?.text) {
      list.push({ role: 'assistant', content: m.answer.text })
    }
  }
  return list
}

async function scrollBottom() {
  await nextTick()
  if (bodyEl.value) bodyEl.value.scrollTop = bodyEl.value.scrollHeight
}
</script>

<style scoped>
.chat-widget {
  position: fixed;
  inset: 0;
  z-index: 1000;
  pointer-events: none;
}
.chat-widget > * { pointer-events: auto; }

/* 悬浮球 */
.chat-fab {
  position: absolute;
  width: 58px;
  height: 58px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(140deg, #ff9a3d 0%, var(--brand) 45%, var(--brand-dark) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 10px 26px rgba(255, 122, 26, 0.38), 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: transform .2s ease, box-shadow .2s ease, left .28s cubic-bezier(.34,1.4,.64,1), top .28s cubic-bezier(.34,1.4,.64,1);
  touch-action: none;
  user-select: none;
  z-index: 3;
}
/* 展开时圆球嵌在面板右下角，作为关闭按钮 */
.chat-fab.fab-active {
  cursor: pointer;
}
.chat-fab:hover {
  transform: scale(1.06);
  box-shadow: 0 14px 32px rgba(255, 122, 26, 0.45), 0 2px 8px rgba(0, 0, 0, 0.12);
}
.chat-fab:active { cursor: grabbing; }
.chat-fab.fab-dragging {
  transition: none;
  transform: scale(1.1);
  cursor: grabbing;
}
.fab-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(255, 122, 26, 0.45);
  animation: fab-pulse 2.4s ease-out infinite;
  pointer-events: none;
}
@keyframes fab-pulse {
  0% { transform: scale(1); opacity: .9; }
  70%, 100% { transform: scale(1.35); opacity: 0; }
}

/* 面板 */
.chat-panel {
  position: absolute;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(16, 24, 40, 0.18);
  overflow: hidden;
  z-index: 1;
}
.chat-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.head-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: linear-gradient(140deg, #ff9a3d, var(--brand-dark));
  display: flex;
  align-items: center;
  justify-content: center;
}
.head-title { font-size: 14px; font-weight: 600; color: var(--text-1); }
.head-sub { font-size: 11px; color: var(--text-3); margin-top: 1px; }
.head-text { flex: 1; min-width: 0; }
.head-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background .15s ease, color .15s ease;
}
.head-btn:hover { background: var(--bg); color: var(--brand); }
.head-close:hover { background: #fdeaea; color: #b42318; }

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--bg);
}
.chat-welcome { text-align: center; padding-top: 22px; }
.welcome-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 10px;
  border-radius: 16px;
  background: var(--brand-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}
.welcome-title { margin: 0; font-size: 15px; font-weight: 600; color: var(--text-1); }
.welcome-sub { margin: 4px 0 14px; font-size: 12px; color: var(--text-3); }
.welcome-chips, .quick { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.quick-chip {
  font-size: 12px;
  padding: 6px 12px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  color: var(--text-2);
  cursor: pointer;
  background: var(--surface);
  transition: all .15s ease;
}
.quick-chip:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-soft);
}

.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.user-text {
  background: linear-gradient(135deg, #ff8f3c, var(--brand));
  color: #fff;
  font-size: 13px;
  line-height: 1.6;
  padding: 9px 13px;
  border-radius: 14px 14px 4px 14px;
  max-width: 78%;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(255, 122, 26, 0.25);
}
.msg.bot { justify-content: flex-start; }
.msg.bot > :deep(*) { width: 100%; }
/* 等待行：三点动画 + 趣味提示语横向排列，不撑满整行 */
.msg.bot:has(.typing) {
  align-items: center;
  gap: 0;
}
.msg.bot:has(.typing) > :deep(.typing) {
  width: auto;
  flex-shrink: 0;
}

/* 流式输出中的临时气泡：与 AnswerBubble 保持一致的观感 */
.streaming-bubble {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 12px 14px;
  box-shadow: var(--shadow-xs);
}
.streaming-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-1);
  white-space: pre-wrap;
  word-break: break-word;
}
/* 打字光标 */
.caret {
  display: inline-block;
  width: 2px;
  height: 13px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--brand);
  animation: caret-blink 0.9s steps(2, start) infinite;
}
@keyframes caret-blink {
  to { visibility: hidden; }
}
.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px 14px 14px 4px;
  padding: 12px 14px;
}
.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-3);
  animation: typing-bounce 1.2s infinite ease-in-out;
}
.typing i:nth-child(2) { animation-delay: .15s; }
.typing i:nth-child(3) { animation-delay: .3s; }
@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: .5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* 等待时的趣味提示语，与三点动画并排 */
.typing-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-3);
  animation: hint-fade .4s ease;
}
@keyframes hint-fade {
  from { opacity: 0; transform: translateY(2px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--surface);
  border-top: 1px solid var(--border);
}
.chat-input-field {
  flex: 1;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 9px 14px;
  font-size: 13px;
  color: var(--text-1);
  outline: none;
  background: var(--bg);
  transition: border-color .15s ease, box-shadow .15s ease;
}
.chat-input-field:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(255, 122, 26, 0.12);
  background: var(--surface);
}
.send-btn {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(140deg, #ff9a3d, var(--brand-dark));
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 122, 26, 0.35);
  transition: transform .15s ease, opacity .15s ease;
}
.send-btn:hover:not(:disabled) { transform: scale(1.08); }
.send-btn:disabled { opacity: .55; cursor: not-allowed; }

.chat-pop-enter-active, .chat-pop-leave-active { transition: opacity .18s ease, transform .18s ease; }
.chat-pop-enter-from, .chat-pop-leave-to { opacity: 0; transform: translateY(10px) scale(.98); }
</style>
