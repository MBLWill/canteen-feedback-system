import request, { getToken } from './request'

export function askChat(question, history) {
  const payload = { question }
  if (Array.isArray(history) && history.length) {
    payload.history = history
  }
  // 智能体多轮工具调用 + LLM 超时 30s，沿用 axios 默认 15s 会被误判成网络错误
  return request.post('/chat/ask', payload, { timeout: 45000 })
}

/**
 * 流式提问（SSE）。
 * <p>
 * 不用 EventSource：它不支持 POST，也不能自定义 Authorization header。
 * 这里用 fetch + ReadableStream 手工解析 SSE。
 *
 * @param {string} question 本轮提问
 * @param {Array}  history  多轮历史
 * @param {Object} handlers { onDelta(text), onDone(answer), onError(err) }
 * @returns {Promise<{answer:Object|null, aborted:boolean}>}
 */
export async function askChatStream(question, history, handlers = {}) {
  const { onDelta, onDone, onError } = handlers
  const payload = { question }
  if (Array.isArray(history) && history.length) {
    payload.history = history
  }

  const controller = new AbortController()
  const resp = await fetch('/api/chat/ask/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      Accept: 'text/event-stream',
      ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {})
    },
    body: JSON.stringify(payload),
    signal: controller.signal
  })

  if (!resp.ok) {
    // 后端对 SSE 异常返回纯文本，这里直接读出来
    let msg = `请求失败（HTTP ${resp.status}）`
    try {
      const t = await resp.text()
      if (t) msg = t
    } catch {
      // 忽略读取失败
    }
    throw new Error(msg)
  }
  if (!resp.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let answer = null
  let aborted = false

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 事件以空行分隔；\r\n 与 \n 都要兼容
      let sep
      while ((sep = buffer.search(/\r?\n\r?\n/)) !== -1) {
        const rawEvent = buffer.slice(0, sep)
        buffer = buffer.slice(sep).replace(/^\r?\n\r?\n/, '')
        const parsed = parseSseEvent(rawEvent)
        if (!parsed) continue

        if (parsed.event === 'delta') {
          try {
            const obj = JSON.parse(parsed.data)
            if (obj.text) onDelta?.(obj.text)
          } catch {
            // 单条解析失败不影响后续
          }
        } else if (parsed.event === 'done') {
          try {
            answer = JSON.parse(parsed.data).answer || null
            onDone?.(answer)
          } catch (e) {
            onError?.(e)
          }
        } else if (parsed.event === 'error') {
          try {
            const obj = JSON.parse(parsed.data)
            onError?.(new Error(obj.message || '流式请求失败'))
          } catch {
            onError?.(new Error('流式请求失败'))
          }
        }
      }
    }
  } catch (e) {
    if (e.name === 'AbortError') {
      aborted = true
    } else {
      throw e
    }
  } finally {
    try {
      reader.releaseLock()
    } catch {
      // 忽略
    }
  }

  return { answer, aborted, abort: () => controller.abort() }
}

/** 解析单个 SSE 事件块，返回 {event, data} */
function parseSseEvent(raw) {
  let event = 'message'
  const dataLines = []
  for (const line of raw.split(/\r?\n/)) {
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }
  if (!dataLines.length) return null
  return { event, data: dataLines.join('\n') }
}

/** 当前推理引擎：agent=智能助手，rule=本地规则引擎 */
export function getChatEngine() {
  return request.get('/chat/engine')
}

export function getChatSuggestions() {
  return request.get('/chat/suggestions')
}

export function rateChat(logId, helpful) {
  return request.put(`/chat/${logId}/feedback`, null, { params: { helpful } })
}

export function getChatLogs(params) {
  return request.get('/chat/logs', { params })
}

export function getChatStats() {
  return request.get('/chat/logs/stats')
}

export function getChatMissTop(limit = 10) {
  return request.get('/chat/logs/miss', { params: { limit } })
}

export function getFaqs(params) {
  return request.get('/faqs', { params })
}

export function createFaq(data) {
  return request.post('/faqs', data)
}

export function updateFaq(id, data) {
  return request.put(`/faqs/${id}`, data)
}

export function deleteFaq(id) {
  return request.delete(`/faqs/${id}`)
}
