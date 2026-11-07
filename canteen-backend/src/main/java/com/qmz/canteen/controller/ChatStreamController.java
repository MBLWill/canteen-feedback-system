package com.qmz.canteen.controller;

import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.ChatRequest;
import com.qmz.canteen.security.LoginUser;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.ChatService;
import com.qmz.canteen.vo.ChatAnswerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 智能客服流式接口（SSE）。
 * <p>
 * 单独成类的原因：SSE 响应的 Content-Type 是 text/event-stream，
 * 若与普通 JSON 接口混在同一个 Controller，一旦抛异常会被
 * {@code GlobalExceptionHandler} 包装成 {@code Result} JSON，
 * 而 text/event-stream 没有对应的消息转换器，会再抛
 * {@code HttpMessageNotWritableException}，把真实错误盖掉。
 * 这里用类级 {@code @ExceptionHandler} 兜住，优先级高于全局处理器。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatStreamController {

    private final ChatService chatService;

    /**
     * SSE 专用线程池。
     * 一次提问 8~15 秒，不能用 Tomcat 工作线程跑，否则并发几个用户就占满。
     */
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "chat-sse");
        t.setDaemon(true);
        return t;
    });

    /**
     * 流式提问。
     * <p>
     * 事件格式：
     * <pre>
     * event: delta   data: {"text":"增量片段"}   ← 可多次
     * event: done    data: {"answer":{...}}      ← 最后一次，含完整答案与卡片
     * event: error   data: {"message":"..."}     ← 出错时
     * </pre>
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody @Valid ChatRequest request) {
        // 关键：UserContext 基于 ThreadLocal，SSE 任务跑在独立线程池里，
        // 子线程读不到主线程写入的登录用户。必须在进入异步前抓取快照。
        LoginUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 0 表示不超时，由 LLM 自身超时控制
        SseEmitter emitter = new SseEmitter(0L);
        EXECUTOR.execute(() -> {
            // 在子线程恢复用户上下文，供 ChatServiceImpl 读取
            UserContext.set(currentUser);
            try {
                ChatAnswerVO answer = chatService.askStream(
                        request.getQuestion(),
                        request.getHistory(),
                        delta -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("delta")
                                        .data(Map.of("text", delta), MediaType.APPLICATION_JSON));
                            } catch (Exception e) {
                                // 客户端断开，停止推送但让后端流程自然结束
                                log.debug("SSE 推送失败（客户端可能已断开）：{}", e.getMessage());
                            }
                        });
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of("answer", answer), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                log.warn("SSE 提问失败：{}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", String.valueOf(e.getMessage())),
                                    MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {
                    // 客户端已断开，忽略
                }
                emitter.complete();
            } finally {
                // 必须清理，否则线程复用时会把身份带给下一个请求
                UserContext.clear();
            }
        });
        return emitter;
    }

    /**
     * 本 Controller 的异常统一返回纯文本，绝不返回 {@code Result} JSON。
     * 覆盖参数校验失败、未登录等进入 emitter 之前就抛出的情况。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.warn("SSE 请求异常：{}", e.getMessage());
        String message = e.getMessage() == null ? "请求失败" : e.getMessage();
        return ResponseEntity.status(500)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message);
    }
}
