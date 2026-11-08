<template>
  <div class="chat-widget">
    <transition name="chat-pop">
      <div
        v-if="open"
        class="chat-panel"
        :class="{ 'panel-docked': docked, 'panel-dragging': isDragging }"
        :style="panelStyle"
      >
        <div class="chat-head" @pointerdown="onHeadDown">
          <div class="head-avatar">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" fill="#fff" stroke="none"/>
            </svg>
          </div>
          <div class="head-text">
            <div class="head-title">{{ assistantTitle }}</div>
            <div class="head-sub">{{ engineLabel }}</div>
          </div>
          <div class="head-actions">
            <button class="head-btn" title="新建会话" @click.stop="newSession">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
                <path d="M12 8v6M9 11h6"/>
              </svg>
            </button>
            <button
              class="head-btn"
              :class="{ 'head-btn-on': docked }"
              :title="docked ? '取消固定侧栏' : '固定到右侧'"
              @click.stop="toggleDock"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="16" rx="4"/>
                <path d="M15 4v16"/>
                <path d="M15 4h2a4 4 0 0 1 4 4v8a4 4 0 0 1-4 4h-2z" fill="currentColor" stroke="none" opacity=".18"/>
              </svg>
            </button>
            <button class="head-btn head-close" title="关闭" @click.stop="closePanel">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M6 6l12 12M18 6L6 18"/>
              </svg>
            </button>
          </div>
        </div>

        <div ref="bodyEl" class="chat-body">
          <div v-if="!messages.length" class="chat-welcome">
            <div class="welcome-icon">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--brand)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
              </svg>
            </div>
            <p class="welcome-title">{{ welcomeTitle }}</p>
            <p class="welcome-sub">{{ welcomeSub }}</p>

            <div class="suggest-head">
              <span class="suggest-label">推荐提问</span>
              <button class="refresh-btn" :disabled="!suggestions.length" @click="refreshSuggestions">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 12a9 9 0 0 0-15.5-6.3L3 8"/>
                  <path d="M3 3v5h5"/>
                  <path d="M3 12a9 9 0 0 0 15.5 6.3L21 16"/>
                  <path d="M21 21v-5h-5"/>
                </svg>
                换一批
              </button>
            </div>
            <div class="suggestion-list">
              <button
                v-for="s in visibleSuggestions"
                :key="s"
                class="suggestion-item"
                @click="send(s)"
              >
                <span class="suggestion-dot"></span>
                <span class="suggestion-text">{{ s }}</span>
                <span class="suggestion-arrow">→</span>
              </button>
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
            :placeholder="inputPlaceholder"
            maxlength="200"
            @keyup.enter="send(draft)"
          >
          <button class="send-btn" :disabled="loading" @click="send(draft)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
            </svg>
          </button>
        </div>

        <div class="chat-quick">
          <button
            v-for="q in quickActions"
            :key="q.label"
            class="quick-btn"
            :class="{ 'quick-danger': q.danger }"
            :disabled="loading"
            @click="onQuick(q)"
          >
            <span class="quick-emoji">{{ q.emoji }}</span>
            <span>{{ q.label }}</span>
          </button>
        </div>
      </div>
    </transition>

    <button
      v-if="!open"
      class="chat-fab"
      :style="fabStyle"
      :title="`打开${assistantTitle}`"
      @click="openPanel"
    >
      <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
        <path d="M8 10h8M8 14h5"/>
      </svg>
      <span class="fab-pulse"></span>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import AnswerBubble from '@/components/chat/AnswerBubble.vue'
import { askChat, askChatStream, getChatEngine, getChatSuggestions } from '@/api/chat'
import { useUserStore } from '@/store/user'

const EDGE = 12
/** 面板尺寸：默认高窄窗口，与之前保持一致 */
const PANEL_W = 560
/** 每页推荐问题数 */
const SUGGESTION_PAGE = 4

const userStore = useUserStore()
const isMerchant = computed(() => userStore.isMerchant)

const open = ref(false)
const docked = ref(localStorage.getItem('canteen_chat_docked') === '1')
const draft = ref('')
const loading = ref(false)
const messages = ref([])
const suggestions = ref([])
const suggestionOffset = ref(0)
const bodyEl = ref(null)
const isDragging = ref(false)
/** 当前推理引擎，仅用于头部文案 */
const engine = ref('agent')
const engineLabel = computed(() => (engine.value === 'agent'
  ? '智能助手 · 实时查库，随问随答'
  : '本地助手 · 菜品 · 档口 · 规则'))

const assistantTitle = computed(() => (isMerchant.value ? '商家经营助手' : '食堂小助手'))
const welcomeTitle = computed(() => (isMerchant.value ? '经营遇到问题？问我吧' : '你好，想问点什么？'))
const welcomeSub = computed(() => (isMerchant.value
  ? '菜品管理、订单处理、销售数据、账号绑定都可以问'
  : '可以直接输入菜名，比如「宫保鸡丁」'))
const inputPlaceholder = computed(() => (isMerchant.value
  ? '输入经营问题，比如「菜品在哪里添加」'
  : '输入菜名或问题…'))

const STUDENT_SUGGESTIONS = ['今天有什么推荐', '哪些菜要避雷', '怎么投诉某个窗口', '食堂几点营业']
const MERCHANT_SUGGESTIONS = ['菜品在哪里添加', '学生下单后怎么处理', '怎么查看销售数据', '怎么绑定邮箱', '怎么给管理员留言']

/** 底部快捷功能按钮：点击即发送对应指令，走智能体实时查库，不死板 */
const quickActions = computed(() => {
  const base = [
    { label: '查订单', emoji: '📄', text: '帮我查一下我的订单', danger: false },
    { label: '查看余额', emoji: '💰', text: '我的账户余额是多少', danger: false },
    { label: '推荐菜品', emoji: '🥘', text: '今天有什么好吃的推荐', danger: false }
  ]
  if (isMerchant.value) {
    base[0] = { label: '待办订单', emoji: '📋', text: '今天有哪些待处理订单', danger: false }
    base[1] = { label: '销售数据', emoji: '📈', text: '帮我看看最近的销售情况', danger: false }
  }
  return [...base, { label: '清空对话', emoji: '🧹', text: '', danger: true }]
})

function onQuick(q) {
  if (q.danger) {
    newSession()
    return
  }
  send(q.text)
}

const visibleSuggestions = computed(() => {
  const list = suggestions.value
  if (!list.length) return []
  const count = Math.min(SUGGESTION_PAGE, list.length)
  return Array.from({ length: count }, (_, i) => list[(suggestionOffset.value + i) % list.length])
})

const vp = reactive({ w: 1280, h: 800 })
const panelPos = reactive({ x: 0, y: 0 })

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

function floatWidth() {
  return Math.min(PANEL_W, vp.w - EDGE * 2)
}

function floatHeight() {
  return Math.max(420, vp.h - 32)
}

function placePanel() {
  panelPos.x = Math.max(EDGE, vp.w - floatWidth() - 24)
  panelPos.y = 16
}

const panelStyle = computed(() => {
  // 固定侧栏：窗口尺寸与浮层完全一致，只是贴在右侧且不能拖动
  if (docked.value) {
    return {
      top: '16px',
      right: '0px',
      width: floatWidth() + 'px',
      height: floatHeight() + 'px',
      left: 'auto',
      borderRadius: '20px 0 0 20px'
    }
  }
  return {
    left: panelPos.x + 'px',
    top: panelPos.y + 'px',
    right: 'auto',
    width: floatWidth() + 'px',
    height: floatHeight() + 'px',
    borderRadius: '20px'
  }
})

const fabStyle = computed(() => ({ right: '32px', bottom: '96px' }))

function onResize() {
  vp.w = window.innerWidth
  vp.h = window.innerHeight
  if (docked.value) return
  panelPos.x = Math.min(Math.max(panelPos.x, EDGE), Math.max(EDGE, vp.w - floatWidth() - EDGE))
  panelPos.y = Math.min(Math.max(panelPos.y, 16), Math.max(16, vp.h - floatHeight() - 16))
}

onMounted(async () => {
  vp.w = window.innerWidth
  vp.h = window.innerHeight
  placePanel()
  window.addEventListener('resize', onResize)
  try {
    const res = await getChatSuggestions()
    suggestions.value = (res.data && res.data.length)
      ? res.data
      : (isMerchant.value ? MERCHANT_SUGGESTIONS : STUDENT_SUGGESTIONS)
  } catch {
    suggestions.value = isMerchant.value ? MERCHANT_SUGGESTIONS : STUDENT_SUGGESTIONS
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
  window.removeEventListener('pointermove', onHeadDrag)
  window.removeEventListener('pointerup', onHeadUp)
  stopThinking()
})

/* ---------------- 面板操作 ---------------- */

function openPanel() {
  open.value = true
  scrollBottom()
}

function closePanel() {
  open.value = false
}

function newSession() {
  messages.value = []
  draft.value = ''
  suggestionOffset.value = 0
  open.value = true
  scrollBottom()
}

function toggleDock() {
  docked.value = !docked.value
  localStorage.setItem('canteen_chat_docked', docked.value ? '1' : '0')
  if (docked.value) {
    open.value = true
  } else {
    // 取消固定后恢复为可拖动的浮层位置
    placePanel()
  }
}

function refreshSuggestions() {
  if (!suggestions.value.length) return
  suggestionOffset.value = (suggestionOffset.value + SUGGESTION_PAGE) % suggestions.value.length
}

/* ---------------- 浮动面板拖动 ---------------- */

let headDrag = null

function onHeadDown(e) {
  // 固定侧栏时不可拖动
  if (docked.value) return
  if (e.button !== undefined && e.button !== 0) return
  if (e.target.closest('button')) return
  e.preventDefault()
  headDrag = { sx: e.clientX, sy: e.clientY, ox: panelPos.x, oy: panelPos.y }
  window.addEventListener('pointermove', onHeadDrag)
  window.addEventListener('pointerup', onHeadUp)
}

function onHeadDrag(e) {
  if (!headDrag) return
  const w = floatWidth()
  const h = floatHeight()
  panelPos.x = Math.min(Math.max(headDrag.ox + e.clientX - headDrag.sx, EDGE), vp.w - w - EDGE)
  panelPos.y = Math.min(Math.max(headDrag.oy + e.clientY - headDrag.sy, EDGE), vp.h - h - EDGE)
  isDragging.value = true
}

function onHeadUp() {
  window.removeEventListener('pointermove', onHeadDrag)
  window.removeEventListener('pointerup', onHeadUp)
  headDrag = null
  isDragging.value = false
}

/* ---------------- 问答 ---------------- */

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
    answer: { text: '', hit: true, items: [], quickReplies: [], sources: null, actions: [] }
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
  cursor: pointer;
  box-shadow: 0 10px 26px rgba(255, 122, 26, 0.38), 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: transform .2s ease, box-shadow .2s ease;
  z-index: 3;
}
.chat-fab:hover {
  transform: scale(1.06);
  box-shadow: 0 14px 32px rgba(255, 122, 26, 0.45), 0 2px 8px rgba(0, 0, 0, 0.12);
}
.chat-fab:active { transform: scale(0.96); }
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
.chat-panel.panel-dragging { transition: none; }

.chat-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  cursor: grab;
  user-select: none;
  touch-action: none;
}
.panel-dragging .chat-head { cursor: grabbing; }
.panel-docked .chat-head { cursor: default; }

.head-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: linear-gradient(140deg, #ff9a3d, var(--brand-dark));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.head-title { font-size: 14px; font-weight: 600; color: var(--text-1); }
.head-sub { font-size: 11px; color: var(--text-3); margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.head-text { flex: 1; min-width: 0; }

.head-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.head-btn {
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
.head-btn-on { background: var(--brand-soft); color: var(--brand-dark); }
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
.welcome-sub { margin: 4px 0 0; font-size: 12px; color: var(--text-3); }

.suggest-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 2px 8px;
}
.suggest-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-3);
}
.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: var(--brand-dark);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 6px;
  transition: background .15s ease, color .15s ease;
}
.refresh-btn:hover:not(:disabled) { background: var(--brand-soft); color: var(--brand); }
.refresh-btn:disabled { opacity: .45; cursor: not-allowed; }
.refresh-btn svg { width: 14px; height: 14px; }

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}
.suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  transition: all .15s ease;
}
.suggestion-item:hover {
  border-color: var(--brand-line);
  background: var(--brand-soft);
  color: var(--brand-dark);
  transform: translateX(2px);
}
.suggestion-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand);
  opacity: .7;
  flex-shrink: 0;
}
.suggestion-text { flex: 1; }
.suggestion-arrow { color: var(--text-3); font-size: 12px; }

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

/* 底部快捷功能按钮栏 */
.chat-quick {
  display: flex;
  gap: 8px;
  padding: 4px 12px 12px;
  background: var(--surface);
  border-top: 1px solid var(--border);
  flex-wrap: wrap;
}
.quick-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: var(--bg);
  color: var(--text-2);
  font-size: 12px;
  cursor: pointer;
  transition: all .15s ease;
}
.quick-btn:hover:not(:disabled) {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-soft);
}
.quick-btn:disabled { opacity: .5; cursor: not-allowed; }
.quick-emoji { font-size: 13px; }
.quick-danger { color: #e0353b; }
.quick-danger:hover:not(:disabled) {
  border-color: #e0353b;
  color: #e0353b;
  background: #fdeaea;
}

.chat-pop-enter-active, .chat-pop-leave-active { transition: opacity .18s ease, transform .18s ease; }
.chat-pop-enter-from, .chat-pop-leave-to { opacity: 0; transform: translateY(10px) scale(.98); }
</style>
