package com.qmz.canteen.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智能体（LLM）配置。全部可在 application.yml 的 canteen.agent 下覆盖。
 * <p>
 * 关闭 enabled 时，ChatService 自动回退到本地规则引擎，保证断网/无 key 也能用。
 */
@Data
@Component
@ConfigurationProperties(prefix = "canteen.agent")
public class AgentProperties {

    /** 总开关：false 时完全走本地规则引擎 */
    private boolean enabled = true;

    /** OpenAI 兼容的 chat/completions 地址 */
    private String baseUrl = "https://api.siliconflow.cn/v1";

    private String apiKey = "";

    private String model = "Qwen/Qwen2.5-7B-Instruct";

    /** 单次 HTTP 请求超时（毫秒） */
    private int timeoutMs = 20000;

    /** 工具调用循环最大轮数，防止模型死循环；多道菜分别加购需要至少 4 轮 */
    private int maxToolRounds = 4;

    /** 单轮对话最多携带的历史消息条数（user+assistant 各算一条） */
    private int historyLimit = 8;

    /** 温度：客服场景要稳定，给低值 */
    private double temperature = 0.2;

    /** 单次回答最多生成 token */
    private int maxTokens = 800;

    /** LLM 失败时是否自动降级到规则引擎 */
    private boolean fallbackToRule = true;

    /** 管理员侧是否允许使用 Agent（关闭可只看规则引擎效果） */
    private boolean enableForAdmin = true;
}
