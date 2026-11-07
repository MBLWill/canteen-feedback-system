package com.qmz.canteen.agent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 调用 OpenAI 兼容的 /chat/completions 接口。
 * 只做「发请求 + 解析 JSON」，不含任何业务逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final AgentProperties props;

    /** 下划线转驼峰，直接映射 OpenAI 的 finish_reason / tool_calls */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private volatile HttpClient httpClient;

    private HttpClient client() {
        HttpClient current = httpClient;
        if (current == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(Math.min(props.getTimeoutMs(), 8000)))
                            .build();
                }
            }
            current = httpClient;
        }
        return current;
    }

    /**
     * 发起一次对话补全请求。
     *
     * @return 解析后的响应；网络/协议失败时抛出 {@link LlmException}
     */
    public LlmProtocol.ChatResponse chat(LlmProtocol.ChatRequest request) {
        String json;
        try {
            json = MAPPER.writeValueAsString(request);
        } catch (Exception e) {
            throw new LlmException("序列化请求失败：" + e.getMessage(), e);
        }
        if (log.isDebugEnabled()) {
            log.debug("LLM 请求体：{}", json.length() > 4000 ? json.substring(0, 4000) + "..." : json);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(props.getBaseUrl()) + "/chat/completions"))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        long start = System.currentTimeMillis();
        HttpResponse<String> response;
        try {
            response = client().send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new LlmException("模型响应超时（" + props.getTimeoutMs() + "ms）", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("模型调用被中断", e);
        } catch (Exception e) {
            throw new LlmException("模型调用失败：" + e.getMessage(), e);
        }

        long cost = System.currentTimeMillis() - start;
        String body = response.body() == null ? "" : response.body();

        if (response.statusCode() != 200) {
            throw new LlmException("模型返回 HTTP " + response.statusCode() + "：" + abbreviate(body, 300), null);
        }

        try {
            JsonNode root = MAPPER.readTree(body);
            // 兼容部分网关把错误包在 200 里
            if (root.has("error") && !root.get("error").isNull()) {
                throw new LlmException("模型返回错误：" + root.get("error").toString(), null);
            }
            LlmProtocol.ChatResponse parsed = MAPPER.treeToValue(root, LlmProtocol.ChatResponse.class);
            if (parsed.getChoices() == null || parsed.getChoices().isEmpty()) {
                throw new LlmException("模型未返回任何候选结果", null);
            }
            log.debug("LLM 调用完成，耗时 {}ms，模型 {}", cost, props.getModel());
            return parsed;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("解析模型响应失败：" + e.getMessage(), e);
        }
    }

    /**
     * 流式对话补全（SSE）。每收到一段增量文本就回调一次 {@code onDelta}。
     * <p>
     * 只用于「最终回答生成」阶段；工具调用轮次仍走非流式 {@link #chat}，
     * 因为工具决策需要完整解析 tool_calls，流式反而更复杂且没有收益。
     *
     * @param request 请求（内部会强制 stream=true）
     * @param onDelta 增量文本回调，可能被调用多次
     * @return 完整拼接后的文本
     */
    public String chatStream(LlmProtocol.ChatRequest request, java.util.function.Consumer<String> onDelta) {
        request.setStream(true);
        String json;
        try {
            json = MAPPER.writeValueAsString(request);
        } catch (Exception e) {
            throw new LlmException("序列化请求失败：" + e.getMessage(), e);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(props.getBaseUrl()) + "/chat/completions"))
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response;
        try {
            response = client().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new LlmException("模型响应超时（" + props.getTimeoutMs() + "ms）", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("模型调用被中断", e);
        } catch (Exception e) {
            throw new LlmException("模型调用失败：" + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            String body = "";
            try (java.io.InputStream in = response.body()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // 读错误体失败不掩盖原始状态码
            }
            throw new LlmException("模型返回 HTTP " + response.statusCode() + "：" + abbreviate(body, 300), null);
        }

        StringBuilder full = new StringBuilder();
        int chunkCount = 0;
        long firstChunkAt = 0;
        long streamStart = System.currentTimeMillis();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }
                String delta = extractDelta(payload);
                if (delta != null && !delta.isEmpty()) {
                    if (chunkCount == 0) {
                        firstChunkAt = System.currentTimeMillis() - streamStart;
                    }
                    chunkCount++;
                    full.append(delta);
                    if (onDelta != null) {
                        onDelta.accept(delta);
                    }
                }
            }
        } catch (Exception e) {
            // 已经吐出去的内容仍然有效，避免整段丢失
            if (full.length() > 0) {
                log.warn("流式读取中断，已收到 {} 字：{}", full.length(), e.getMessage());
                return full.toString();
            }
            throw new LlmException("读取流式响应失败：" + e.getMessage(), e);
        }

        log.info("LLM 流式完成：分片={} 首片={}ms 总耗时={}ms 字数={}",
                chunkCount, firstChunkAt, System.currentTimeMillis() - streamStart, full.length());

        if (full.length() == 0) {
            throw new LlmException("模型未返回任何内容", null);
        }
        return full.toString();
    }

    /** 从一条 SSE data 里取出增量文本；解析失败返回 null 而不是抛错 */
    private String extractDelta(String payload) {
        try {
            JsonNode root = MAPPER.readTree(payload);
            if (root.has("error") && !root.get("error").isNull()) {
                throw new LlmException("模型返回错误：" + root.get("error").toString(), null);
            }
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode delta = choices.get(0).get("delta");
            if (delta == null) {
                return null;
            }
            JsonNode content = delta.get("content");
            return content == null || content.isNull() ? null : content.asText();
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.debug("SSE 片段解析失败：{}", abbreviate(payload, 120));
            return null;
        }
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    /** LLM 调用失败的统一异常，便于上层决定是否降级 */
    public static class LlmException extends RuntimeException {
        public LlmException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
