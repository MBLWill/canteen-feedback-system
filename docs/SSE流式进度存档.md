# SSE 流式输出 · 完成报告

> 最后更新：2026-10-28 09:35
> 状态：**已完成并验证通过**

---

## 一、任务背景

GLM-5.3-Flash 单次回答需要 **8~15 秒**（含思考链），课堂演示时观众要干等。
目标：改成 SSE 流式，让文字逐字出现，改善感知。

---

## 二、实测结果

### 2.1 流式已生效

| 问题 | delta 数 | 总耗时 | 答案 |
| --- | --- | --- | --- |
| 宫保鸡丁多少钱 | 40 | 8905ms | ✅ 12元，川味小炒01窗口 |
| 麻婆豆腐多少钱 | 45 | 7689ms | ✅ 8元，评分4.43 |
| 食堂几点营业 | 66 | 7459ms | ✅ 06:30-09:00 |
| 今天有什么推荐 | 74 | 13469ms | ✅ 5道高分菜 |
| 佛跳墙多少钱 | 36 | 6292ms | ✅ 如实说没有 |

**全部 5/5 通过**，delta 数 36~74，说明文字是逐字推送的。

### 2.2 关键性能数据

| 指标 | 数值 |
| --- | --- |
| **首个 delta 到达** | **5608ms** |
| done 事件到达 | 8181ms |
| 上游模型首片 | 660~1945ms |
| 流式 vs 非流式总耗时 | 6974ms vs 12035ms |

### 2.3 时间都花在哪了（重要）

实测拆解一次完整请求：

```
0ms          ── 请求开始
   ↓
~5000ms      ── 第1轮：模型决策「要调哪个工具」（非流式，无法优化）
   ↓
+200ms       ── 工具执行（查库，很快）
   ↓
~5600ms      ── 推「正在查询食堂数据…」+ 开始流式
   ↓
+2500ms      ── 模型逐字生成答案
   ↓
8181ms       ── done
```

**结论**：首字慢的根因不是流式没做好，而是**工具决策轮本身要 5 秒**。
这一段必须等模型返回 `tool_calls`，无法流式化。

**已做的优化**：在工具决策前先推一条「正在查询食堂数据…」，
让首字从 9.2s 降到 **5.6s**——用户至少知道"在查了"，不会以为卡死。

---

## 三、实现方案

### 3.1 核心设计：只流式「总结」阶段

```
第1轮：模型决策 → 返回 tool_calls        【非流式】5秒
         ↓
       执行工具查库                       【本地】0.2秒
         ↓
第2轮：模型读结果 → 生成自然语言回答      【流式】2.5秒 ← 只有这段流式
```

**为什么不全程流式**：工具决策需要完整解析 `tool_calls` 结构，
流式反而更复杂且没有收益（那段不产生用户可见文本）。

### 3.2 关键代码

**`LlmClient.chatStream()`**
```java
public String chatStream(ChatRequest request, Consumer<String> onDelta)
```
- 强制 `stream=true`，用 `HttpResponse.BodyHandlers.ofInputStream()` 逐行读
- 解析 `data:` 行，取 `choices[0].delta.content`
- 断流时保留已收内容

**`AgentOrchestrator`**
```java
public AgentResult chatStream(question, history, onDelta)   // 流式入口
private AgentResult chatInternal(..., onDelta)              // 共用实现
private AgentResult summarize(..., onDelta)                 // 流式在此
```

关键改动——工具执行完立刻转流式：
```java
if (onDelta != null && !toolResults.isEmpty()) {
    onDelta.accept("\n");
    return summarize(messages, calledTools, toolResults, ..., onDelta);
}
```

**`ChatStreamController`**（独立类）
```java
@PostMapping(value = "/chat/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter askStream(@RequestBody @Valid ChatRequest request)
```

事件格式：
```
event:delta   data:{"text":"增量片段"}
event:done    data:{"answer":{...完整答案与卡片}}
event:error   data:{"message":"..."}
```

**前端 `api/chat.js`**
```js
export async function askChatStream(question, history, { onDelta, onDone, onError })
```
用 `fetch` + `ReadableStream`（EventSource 不支持 POST + 自定义 header）。

**前端 `ChatWidget.vue`**
- 流式时显示 `.streaming-bubble` + 闪烁光标
- 收到 done 后切换成 `AnswerBubble` 渲染卡片
- 失败时退回非流式接口兜底

---

## 四、踩过的坑（4 个）

### 坑 1：ThreadLocal 跨线程丢失（最关键）

**现象**：SSE 端点一直返回「请先登录后再提问」，但同一 token 打普通接口正常。

**根因**：`UserContext` 基于 `ThreadLocal`，而 SSE 任务跑在独立线程池
`chat-sse` 里，子线程读不到主线程写入的登录用户。

**解决**：进入异步前抓快照，子线程里恢复：
```java
LoginUser currentUser = UserContext.get();   // 主线程抓取
EXECUTOR.execute(() -> {
    UserContext.set(currentUser);            // 子线程恢复
    try { ... } finally { UserContext.clear(); }  // 必须清理，防线程复用串号
});
```

### 坑 2：全局异常处理器把 SSE 响应搞崩

**现象**：SSE 直接断开，客户端收不到任何事件。

**日志**：
```
HttpMessageNotWritableException: No converter for [class Result]
with preset Content-Type 'text/event-stream'
```

**根因**：`GlobalExceptionHandler` 把异常包装成 `Result` JSON，
但 `text/event-stream` 没有对应转换器，抛第二次异常盖掉原始错误。

**解决**：SSE 端点独立成 `ChatStreamController`，配类级
`@ExceptionHandler` 返回 `ResponseEntity<String>` 纯文本。
类级处理器优先级高于全局。

> 不要往 `GlobalExceptionHandler` 加第二个 `@ExceptionHandler(Exception.class)`，
> Spring 启动会报 "Ambiguous @ExceptionHandler"。

### 坑 3：只有 1 个 delta，等于没流式

**现象**：SSE 通了，但只收到 1 个巨大的 delta。

**排查过程**：
1. 先怀疑上游不流式 → 加日志打印分片数
2. 日志显示 `Agent 第 2 轮：finish=stop toolCalls=0` → 模型直接返回了完整文本
3. 定位到主循环：工具执行后 `continue` 回到 auto 循环，
   模型下一轮直接给文本，走的是**非流式分支**

**解决**：工具执行完立刻转入 `summarize`（流式），不再回主循环。

### 坑 4：pwsh 批量替换破坏文件编码

**现象**：`Set-Content -NoNewline` 替换后中文全变乱码（`锛`/`鍑`/`妯`）。

**教训**：
- 不要用 `Get-Content -Raw` + `Set-Content` 改含中文的 Java 文件
- 用 `edit` 工具做精确替换
- 改完验证：`[IO.File]::ReadAllText($p,[Text.Encoding]::UTF8) -match '锛|鍑|妯'`

---

## 五、验证清单

- [x] 流式输出生效（delta 36~74 个）
- [x] 首字时间 5608ms（有「正在查询」提示）
- [x] 答案准确（价格/评分/窗口全部来自 DB）
- [x] 卡片正常（done 事件里含 items）
- [x] 多轮流式正确（指代「它」「这家」）
- [x] 降级正常（Key 失效时 581ms 返回，1 个 delta + done）
- [x] 非流式接口未被改坏（回归 8/8）
- [x] 前端构建通过
- [x] 数据已还原（52菜/12档/762评/24库）

---

## 六、怎么用

### 启动

```bash
net start MySQL80
cd canteen-backend && mvn spring-boot:run
cd canteen-frontend && npm run dev
```

### 测试流式（curl）

```powershell
# 登录拿 token
$b = @{username='student01';password='123456'} | ConvertTo-Json
$tok = (Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/auth/login' -Method Post -Body $b -ContentType 'application/json').data.token

# 流式提问
'{"question":"宫保鸡丁多少钱"}' | Out-File -Encoding utf8 -NoNewline body.json
curl.exe -N -s -X POST 'http://127.0.0.1:8080/api/chat/ask/stream' `
  -H "Authorization: Bearer $tok" -H "Content-Type: application/json" `
  -H "Accept: text/event-stream" --data-binary "@body.json" --max-time 90
```

### 前端

打开 http://localhost:5173，点右下角悬浮球，输入问题即可看到逐字输出。

---

## 七、后续可优化（可选）

| 优化点 | 预期收益 | 难度 |
| --- | --- | --- |
| 工具决策轮也流式（先吐「正在查」再由工具结果补全） | 首字降到 ~1s | 高，需重构协议 |
| 缓存高频问题的工具结果 | 跳过工具轮，首字 ~1s | 中 |
| 换更快的模型（如 glm-4-flash） | 首字 2~3s | 低，改配置 |
| 预设常见问题的快捷回答（不调模型） | 秒回 | 低 |

**建议**：课堂演示当前方案够用（首字 5.6s 且立刻有「正在查询」提示），
若要更快，最简单的是换个更快的模型。

---

## 八、文件清单

### 新增
```
canteen-backend/src/main/java/com/qmz/canteen/controller/ChatStreamController.java
```

### 修改
```
canteen-backend/src/main/java/com/qmz/canteen/agent/LlmClient.java           + chatStream() + 分片日志
canteen-backend/src/main/java/com/qmz/canteen/agent/AgentOrchestrator.java   + chatStream/chatInternal/summarize(onDelta) + 状态提示
canteen-backend/src/main/java/com/qmz/canteen/service/ChatService.java       + askStream()
canteen-backend/src/main/java/com/qmz/canteen/service/impl/ChatServiceImpl.java + askStream/tryAgentStream/toAnswer
canteen-backend/src/main/java/com/qmz/canteen/controller/ChatController.java  - 移除 SSE 端点
canteen-frontend/src/api/chat.js                                              + askChatStream()
canteen-frontend/src/components/ChatWidget.vue                                + 流式渲染 + 光标动画
```
