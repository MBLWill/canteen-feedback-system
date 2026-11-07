package com.qmz.canteen.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体编排：把用户问题 + 多轮历史 + 工具集交给模型，循环执行工具调用，
 * 直到模型给出最终回答或达到轮数上限。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AgentProperties props;
    private final LlmClient llmClient;
    private final AgentTools tools;

    /**
     * 系统提示词。
     * <p>
     * 实测结论（务必先读再改）：
     * 1. Qwen2.5-7B 对「【分节标题】+ 大量强指令（严禁/必须/最高原则）」这种结构化提示词
     *    会退化成「只说话不调工具」（finish=stop, toolCalls=0），例如问「宫保鸡丁多少钱」
     *    会直接编造价格。改成紧凑自然语言后恢复正常。
     * 2. 即便提示词不变，带多轮历史时模型仍有约 30%~50% 概率「忘记调工具」而直接作答。
     *    这是小模型的固有抖动，靠调提示词无法根治。
     * 3. 因此真正的兜底在 {@link #chatWithRequired}：模型第一轮没调工具且问题看起来
     *    需要数据时，用 tool_choice=required 强制重试一次。提示词只需保持简短。
     */
    private static final String SYSTEM_PROMPT = """
            你是食堂小助手，帮学生查校园食堂的菜品、档口和规则。
            本校食堂分南北两区：南区食堂有两层（1 楼、2 楼），北区食堂只有一层（1 楼）。
            遇到涉及菜品、价格、评分、档口、推荐、制度、公告的问题，先调用工具查询，
            再根据工具返回的真实数据回答，不要凭记忆编造价格、评分或菜名。
            问「南区/北区有什么」「几楼有什么」「哪个窗口在哪层」时，用 list_stalls
            传 area（south/north）和 floor（1/2）过滤后再回答。
            制度类问题（怎么投诉、怎么预约、评价能不能改）以 search_faq 返回的 answer 为准，
            按它写的做法和入口回答，不要自己另编一套流程，也不要否定知识库里说的做法。
            工具查不到就如实说没有找到。回答用中文，简洁口语化，120 字以内，
            涉及具体菜品时点出菜名、窗口和价格。
            """;

    /** 收敛阶段追加的指令 */
    private static final String SUMMARIZE_INSTRUCTION =
            "请直接根据上面工具返回的真实数据回答用户的问题，不要再调用工具。"
                    + "如果是制度类问题，请按知识库 answer 里写的做法和入口回答，不要改写成别的流程。";

    /**
     * 执行一次对话（非流式）。
     */
    public AgentResult chat(String question, List<Map<String, String>> history) {
        return chatInternal(question, history, null);
    }

    /**
     * 执行一次对话（流式）。
     * <p>
     * 工具调用轮次仍走非流式（决策需要完整 tool_calls，且很快），
     * 只有最后的「总结回答」阶段用 SSE 逐字回调，
     * 这样用户能立刻看到文字，而不是干等 8~15 秒。
     *
     * @param onDelta 增量文本回调，可能被调用多次；可为 null 表示不流式
     */
    public AgentResult chatStream(String question, List<Map<String, String>> history,
                                  java.util.function.Consumer<String> onDelta) {
        return chatInternal(question, history, onDelta);
    }

    private AgentResult chatInternal(String question, List<Map<String, String>> history,
                                     java.util.function.Consumer<String> onDelta) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        // 只保留最近 N 条历史，避免 prompt 过长
        if (history != null && !history.isEmpty()) {
            int from = Math.max(0, history.size() - props.getHistoryLimit());
            for (Map<String, String> h : history.subList(from, history.size())) {
                String role = h.get("role");
                String content = h.get("content");
                if (role == null || content == null || content.isBlank()) {
                    continue;
                }
                // 只接受 user / assistant，防止客户端伪造 system 角色提权
                if (!"user".equals(role) && !"assistant".equals(role)) {
                    continue;
                }
                messages.add(Map.of("role", role, "content", content));
            }
        }
        messages.add(Map.of("role", "user", "content", question));

        List<Map<String, Object>> toolDefs = tools.definitions();
        List<String> calledTools = new ArrayList<>();
        // 工具名 → 最后一次执行结果，供上层渲染卡片（卡片数据取自 DB，不取自模型）
        Map<String, Object> toolResults = new LinkedHashMap<>();
        int totalPrompt = 0;
        int totalCompletion = 0;

        for (int round = 0; round < props.getMaxToolRounds(); round++) {
            LlmProtocol.ChatRequest request = new LlmProtocol.ChatRequest();
            request.setModel(props.getModel());
            request.setMessages(messages);
            request.setTools(toolDefs);
            request.setToolChoice("auto");
            request.setTemperature(props.getTemperature());
            request.setMaxTokens(props.getMaxTokens());

            LlmProtocol.ChatResponse response = llmClient.chat(request);
            if (response.getUsage() != null) {
                totalPrompt += nz(response.getUsage().getPromptTokens());
                totalCompletion += nz(response.getUsage().getCompletionTokens());
            }

            LlmProtocol.Choice choice = response.getChoices().get(0);
            LlmProtocol.Message msg = choice.getMessage();
            if (msg == null) {
                throw new LlmClient.LlmException("模型返回空消息", null);
            }

            List<LlmProtocol.ToolCall> calls = msg.getToolCalls();
            boolean wantsTool = calls != null && !calls.isEmpty();
            log.info("Agent 第 {} 轮：finish={} toolCalls={} contentLen={}", round + 1,
                    choice.getFinishReason(), wantsTool ? calls.size() : 0,
                    msg.getContent() == null ? 0 : msg.getContent().length());

            if (!wantsTool) {
                String content = msg.getContent() == null ? "" : msg.getContent().trim();
                // 关键兜底：首轮没调工具、但问题明显需要数据时，用 tool_choice=required 强制查一次库。
                // 小模型带多轮历史时有 30%~50% 概率「忘了调工具」直接编造（实测：宫保鸡丁报 8 元，
                // 实际 12 元），靠调提示词治不好，必须在协议层强制。
                if (round == 0 && calledTools.isEmpty() && needsData(question)) {
                    log.info("首轮未调用工具但问题需要数据，强制 tool_choice=required 重试");
                    LlmProtocol.ChatResponse forced = chatWithRequired(messages, toolDefs);
                    if (forced.getUsage() != null) {
                        totalPrompt += nz(forced.getUsage().getPromptTokens());
                        totalCompletion += nz(forced.getUsage().getCompletionTokens());
                    }
                    List<LlmProtocol.ToolCall> forcedCalls = forced.getChoices().get(0).getMessage().getToolCalls();
                    if (forcedCalls != null && !forcedCalls.isEmpty()) {
                        appendAssistantAndRunTools(messages, forcedCalls, calledTools, toolResults, question);
                        // 工具结果已回填，直接进入「只总结、不再调工具」的收敛阶段，
                        // 不能再走 auto 循环，否则模型又会跳过工具直接作答。
                        return summarize(messages, calledTools, toolResults,
                                totalPrompt, totalCompletion, onDelta);
                    }
                }
                if (content.isEmpty()) {
                    throw new LlmClient.LlmException("模型返回空回答", null);
                }
                // 非流式路径已经拿到完整文本；若调用方要求流式，这里补一次回调保证前端一致
                if (onDelta != null) {
                    onDelta.accept(sanitize(content));
                }
                return new AgentResult(sanitize(content), calledTools, toolResults,
                        totalPrompt, totalCompletion, round + 1);
            }

            // 流式场景：首轮工具决策要 3~6 秒，这段时间用户会盯着空白。
            // 先推一条状态提示，让用户知道「在查了」，而不是以为卡住了。
            if (onDelta != null && round == 0 && wantsTool) {
                onDelta.accept("正在查询食堂数据…");
            }

            // 把 assistant 的 tool_calls 原样放回对话，并执行工具回填结果
            appendAssistantAndRunTools(messages, calls, calledTools, toolResults, question);

            // 流式场景：工具已经查完，数据齐了，剩下的只是「组织语言」。
            // 若继续走 auto 循环，模型会在下一轮直接返回完整文本（非流式），
            // 前端就只能收到一次性的一大段，失去流式意义。
            // 所以这里立刻转入 summarize，用 SSE 逐字生成。
            if (onDelta != null && !toolResults.isEmpty()) {
                // 用换行把「正在查询…」与正文分开，前端渲染更自然
                onDelta.accept("\n");
                return summarize(messages, calledTools, toolResults,
                        totalPrompt, totalCompletion, onDelta);
            }
        }
        // 达到轮数上限仍未收敛：用最后拿到的工具结果再逼一次纯文本回答
        log.warn("工具调用达到上限 {} 轮，强制收敛", props.getMaxToolRounds());
        return summarize(messages, calledTools, toolResults, totalPrompt, totalCompletion, onDelta);
    }

    /**
     * 收敛阶段：工具结果已经回填进 messages，这里只让模型「读结果并总结」，
     * 不再提供 tools，从协议层杜绝它继续调用或跳过数据作答。
     * <p>
     * onDelta 不为 null 时走 SSE 流式，边生成边回调。
     */
    private AgentResult summarize(List<Map<String, Object>> messages,
                                  List<String> calledTools,
                                  Map<String, Object> toolResults,
                                  int promptTokens,
                                  int completionTokens,
                                  java.util.function.Consumer<String> onDelta) {
        List<Map<String, Object>> finalMessages = new ArrayList<>(messages);
        finalMessages.add(Map.of("role", "user", "content", SUMMARIZE_INSTRUCTION));

        LlmProtocol.ChatRequest req = new LlmProtocol.ChatRequest();
        req.setModel(props.getModel());
        req.setMessages(finalMessages);
        req.setTemperature(props.getTemperature());
        req.setMaxTokens(props.getMaxTokens());
        // 不传 tools，模型只能输出文本

        String content;
        if (onDelta != null) {
            // 流式：边生成边把增量推给前端。
            // 注意增量是原始 Markdown，前端按纯文本渲染会露出 ** 等符号，
            // 因此这里把「已输出长度」做一次差量清洗：每次只推新增的干净片段。
            StringBuilder raw = new StringBuilder();
            StringBuilder emitted = new StringBuilder();
            content = llmClient.chatStream(req, delta -> {
                raw.append(delta);
                String cleaned = sanitize(raw.toString());
                if (cleaned.length() > emitted.length()) {
                    String piece = cleaned.substring(emitted.length());
                    emitted.append(piece);
                    onDelta.accept(piece);
                }
            });
            content = sanitize(content);
            // 清洗后可能比已推内容更短（例如尾部 ** 被吃掉），把差额补上
            if (content.length() > emitted.length()) {
                onDelta.accept(content.substring(emitted.length()));
            }
        } else {
            LlmProtocol.ChatResponse resp = llmClient.chat(req);
            if (resp.getUsage() != null) {
                promptTokens += nz(resp.getUsage().getPromptTokens());
                completionTokens += nz(resp.getUsage().getCompletionTokens());
            }
            content = resp.getChoices().get(0).getMessage().getContent();
        }

        if (content == null || content.isBlank()) {
            throw new LlmClient.LlmException("模型未能给出最终回答", null);
        }
        return new AgentResult(sanitize(content), calledTools, toolResults,
                promptTokens, completionTokens, props.getMaxToolRounds() + 1);
    }

    /**
     * 把模型给出的 tool_calls 追加到对话，逐个执行并把结果回填为 role=tool 消息。
     *
     * @param rawQuestion 用户原始提问，用于在模型截短关键词时做兜底检索
     */
    private void appendAssistantAndRunTools(List<Map<String, Object>> messages,
                                            List<LlmProtocol.ToolCall> calls,
                                            List<String> calledTools,
                                            Map<String, Object> toolResults,
                                            String rawQuestion) {
        Map<String, Object> assistantMsg = new LinkedHashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", "");
        assistantMsg.put("tool_calls", calls.stream().map(c -> {
            Map<String, Object> tc = new LinkedHashMap<>();
            tc.put("id", c.getId());
            tc.put("type", "function");
            tc.put("function", Map.of(
                    "name", c.getFunction().getName(),
                    "arguments", c.getFunction().getArguments()
            ));
            return tc;
        }).toList());
        messages.add(assistantMsg);

        for (LlmProtocol.ToolCall call : calls) {
            String name = call.getFunction().getName();
            String argJson = call.getFunction().getArguments();
            calledTools.add(name);

            Object result;
            try {
                Map<String, Object> args = parseArgs(argJson);
                result = tools.execute(name, args);

                // 兜底：模型有时会把「某道菜」误判成「新菜榜单」，或把菜名截短
                // （「验证用新菜」→「新菜」）。只要原始提问里出现了完整菜名，
                // 就以精确匹配结果为准，避免答非所问。
                Object byRaw = tools.searchByRawQuestion(rawQuestion, 5);
                if (!isEmptyDishResult(byRaw)) {
                    if (!"search_dishes".equals(name)) {
                        log.info("模型调用 {} 未覆盖具体菜品，用原始提问精确命中并覆盖", name);
                    } else {
                        log.info("用原始提问精确命中菜品，覆盖模型关键词「{}」的检索结果", args.get("keyword"));
                    }
                    result = byRaw;
                    // 卡片也按菜品展示
                    toolResults.put("search_dishes", byRaw);
                }

                toolResults.put(name, result);
                if (log.isDebugEnabled()) {
                    String payload = JSON.writeValueAsString(result);
                    log.debug("工具 {} 参数={} 结果={}", name, argJson,
                            payload.substring(0, Math.min(400, payload.length())));
                }
            } catch (Exception e) {
                log.warn("工具 {} 执行失败：{}", name, e.getMessage());
                result = Map.of("error", "工具执行失败：" + e.getMessage());
            }

            String payload;
            try {
                payload = JSON.writeValueAsString(result);
            } catch (Exception e) {
                payload = "{\"error\":\"结果序列化失败\"}";
            }
            if (payload.length() > 6000) {
                payload = payload.substring(0, 6000) + "...(已截断)";
            }

            Map<String, Object> toolMsg = new LinkedHashMap<>();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", call.getId());
            toolMsg.put("content", payload);
            messages.add(toolMsg);
        }
    }

    /** 判断工具结果里是否没有任何菜品 */
    private boolean isEmptyDishResult(Object result) {
        if (!(result instanceof Map<?, ?> map)) {
            return true;
        }
        Object dishes = map.get("dishes");
        return !(dishes instanceof List<?> list) || list.isEmpty();
    }

    /** 用 tool_choice=required 强制模型必须调用一次工具 */
    private LlmProtocol.ChatResponse chatWithRequired(List<Map<String, Object>> messages,
                                                      List<Map<String, Object>> toolDefs) {
        LlmProtocol.ChatRequest req = new LlmProtocol.ChatRequest();
        req.setModel(props.getModel());
        req.setMessages(messages);
        req.setTools(toolDefs);
        req.setToolChoice("required");
        req.setTemperature(props.getTemperature());
        req.setMaxTokens(props.getMaxTokens());
        return llmClient.chat(req);
    }

    /** 判断问题是否属于「必须查库」的类型，避免对纯闲聊也强制调工具 */
    static boolean needsData(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String q = question.trim();
        // 纯招呼/感谢类不强制
        if (q.length() <= 6 && (q.contains("你好") || q.contains("您好") || q.contains("谢谢")
                || q.contains("再见") || q.equalsIgnoreCase("hi") || q.equalsIgnoreCase("hello"))) {
            return false;
        }
        String[] keywords = {"多少钱", "价格", "几块", "好吃", "评分", "推荐", "窗口", "档口", "菜",
                "营业", "几点", "投诉", "预约", "规则", "公告", "通知", "避雷", "新菜", "招牌",
                "卫生", "怎么样", "是什么", "有什么", "在哪", "哪个"};
        for (String k : keywords) {
            if (q.contains(k)) {
                return true;
            }
        }
        // 太短又没关键词的，交给模型自由判断
        return q.length() >= 8;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return JSON.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("工具参数解析失败：{}", json);
            return Map.of();
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /**
     * 清洗模型输出：去掉 Markdown 与 emoji。
     * 不同模型风格差异很大：Qwen 偶尔输出 **加粗**，智谱 glm 还会输出
     * 「**菜名**」+ 列表 + 💰📍⭐ 这类 emoji。前端按纯文本分行渲染，
     * 这里统一做一次兜底，避免符号露出来。
     */
    static String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")            // **加粗**
                .replaceAll("__(.+?)__", "$1")                     // __加粗__
                .replaceAll("(?<![\\w*])\\*(?!\\s)([^*\\n]+?)\\*(?![\\w*])", "$1") // *斜体*
                .replaceAll("`{1,3}([^`]*)`{1,3}", "$1")           // 行内代码/代码块
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")         // 标题
                .replaceAll("(?m)^\\s*[-*+]\\s+", "· ")            // 无序列表
                .replaceAll("(?m)^\\s*>\\s*", "")                  // 引用块
                .replaceAll("(?m)^\\s*([-*_])\\1{2,}\\s*$", "")    // 分隔线
                .replaceAll("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}]", "") // emoji
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 编排结果。
     *
     * @param text             模型最终回答
     * @param toolsCalled      本次调用过的工具名
     * @param toolResults      工具名 → 原始结果（供卡片渲染，保证数字来自 DB）
     * @param promptTokens     累计输入 token
     * @param completionTokens 累计输出 token
     * @param rounds           实际对话轮数
     */
    public record AgentResult(String text, List<String> toolsCalled, Map<String, Object> toolResults,
                              int promptTokens, int completionTokens, int rounds) {
    }
}
