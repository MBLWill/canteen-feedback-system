package com.qmz.canteen.agent;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议的请求/响应结构（只保留本项目用到的字段）。
 */
public final class LlmProtocol {

    private LlmProtocol() {
    }

    @Data
    public static class ChatRequest {
        private String model;
        private List<Map<String, Object>> messages;
        private List<Map<String, Object>> tools;
        private String toolChoice = "auto";
        private double temperature = 0.2;
        private int maxTokens = 800;
        private boolean stream = false;
    }

    @Data
    public static class ChatResponse {
        private String id;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finishReason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        private List<ToolCall> toolCalls;
        /**
         * 智谱 glm 系列会返回思维链。这里只接收不读取：
         * 一方面避免反序列化报错，另一方面回传 assistant 消息时不能带上它，
         * 否则部分网关会拒绝请求。
         */
        private String reasoningContent;
    }

    @Data
    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;
    }

    @Data
    public static class FunctionCall {
        private String name;
        private String arguments;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
