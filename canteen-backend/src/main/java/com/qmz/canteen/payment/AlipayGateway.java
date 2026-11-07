package com.qmz.canteen.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmz.canteen.config.CommerceProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 支付宝电脑网站支付（alipay.trade.page.pay），RSA2 自签，不依赖官方 SDK。
 * 本机开发收不到异步通知时，靠 {@link #query} 主动查单确认支付结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayGateway implements PayGateway {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CommerceProperties props;

    private PrivateKey privateKey;
    private PublicKey alipayPublicKey;
    private volatile boolean ready;
    /** 平台连通性自检结果：网关返回可识别的业务响应才算健康 */
    private volatile boolean healthy;
    private volatile long healthyCheckedAt;
    /** 自检结果缓存时长，避免每次下单都打一次网络请求 */
    private static final long HEALTH_TTL_MS = 60_000L;

    @PostConstruct
    void init() {
        CommerceProperties.Alipay cfg = alipay();
        if (!cfg.configured()) {
            log.warn("支付宝配置不完整（app-id/私钥/公钥/网关），通道不可用");
            return;
        }
        try {
            privateKey = AlipayCrypto.privateKey(cfg.getPrivateKey());
            alipayPublicKey = AlipayCrypto.publicKey(cfg.getAlipayPublicKey());
            // 签一次证明私钥可用；公钥格式是否正确在解析时已确认
            AlipayCrypto.sign(Map.of("probe", "1"), privateKey);
            ready = true;
            log.info("支付宝密钥加载成功，网关 {}，appId {}（连通性将在结算前自检）", cfg.getGateway(), cfg.getAppId());
            probeAsync();
        } catch (Exception e) {
            ready = false;
            log.error("支付宝密钥加载失败，通道降级为不可用：{}", e.getMessage());
        }
    }

    /** 用一笔必然不存在的订单查单，借响应码验证 app_id/签名/网关是否真的能用 */
    private void probeAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                checkHealth();
                log.info("支付宝自检完成：healthy={}（false 时结算自动降级为 mock，不影响使用）", healthy);
            } catch (Exception e) {
                log.warn("支付宝自检请求失败（不影响启动）：{}", e.getMessage());
            }
        });
    }

    @Override
    public boolean ready() {
        return ready;
    }

    @Override
    public boolean healthy() {
        if (!ready) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - healthyCheckedAt > HEALTH_TTL_MS) {
            checkHealth();
        }
        return healthy;
    }

    /**
     * 真实连通性自检：探针查单返回 WAITING(交易不存在) / SUCCESS / CLOSED 都说明
     * 网关认了这次请求（密钥、appId 正确且平台正常）；UNKNOWN 则多为签名错误、
     * appId 无权限、沙箱 SYSTEM_ERROR 或网络不通，此时判定通道不可用。
     */
    private synchronized void checkHealth() {
        long now = System.currentTimeMillis();
        if (now - healthyCheckedAt <= HEALTH_TTL_MS && healthyCheckedAt > 0) {
            return;
        }
        try {
            PayTradeState state = query("PROBE" + now).state();
            healthy = state != PayTradeState.UNKNOWN;
            healthyCheckedAt = now;
            if (!healthy) {
                log.warn("支付宝网关自检未通过（查单结果 {}），结算将自动降级为 mock 通道", state);
            }
        } catch (Exception e) {
            healthy = false;
            healthyCheckedAt = now;
            log.warn("支付宝网关自检异常，结算将自动降级为 mock 通道：{}", e.getMessage());
        }
    }

    private CommerceProperties.Alipay alipay() {
        return props.getPay().getAlipay();
    }

    @Override
    public String channel() {
        return "alipay";
    }

    @Override
    public String payUrl(PayOrder order) {
        CommerceProperties.Alipay cfg = alipay();
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", cfg.getAppId());
        params.put("method", "alipay.trade.page.pay");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", cfg.getSignType());
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP));
        params.put("version", "1.0");
        String notifyUrl = props.getPay().notifyUrl();
        if (notifyUrl != null) {
            params.put("notify_url", notifyUrl);
        }
        if (props.getPay().getReturnUrl() != null && !props.getPay().getReturnUrl().isBlank()) {
            params.put("return_url", props.getPay().getReturnUrl());
        }
        params.put("biz_content", "{"
                + "\"out_trade_no\":\"" + order.getPayNo() + "\""
                + ",\"total_amount\":\"" + order.getAmount().toPlainString() + "\""
                + ",\"subject\":\"" + escape(order.getSubject()) + "\""
                + ",\"product_code\":\"FAST_INSTANT_TRADE_PAY\""
                + "}");

        String sign = AlipayCrypto.sign(params, privateKey);
        String query = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return cfg.getGateway() + "?" + query + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);
    }

    @Override
    public PayTradeResult query(String payNo) {
        CommerceProperties.Alipay cfg = alipay();
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", cfg.getAppId());
        params.put("method", "alipay.trade.query");
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", cfg.getSignType());
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP));
        params.put("version", "1.0");
        params.put("biz_content", "{\"out_trade_no\":\"" + payNo + "\"}");

        String sign = AlipayCrypto.sign(params, privateKey);
        String form = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"))
                + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.getGateway()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode result = root.get("alipay_trade_query_response");
            if (result == null || result.isNull()) {
                log.warn("支付宝查单响应异常：{}", abbreviate(response.body()));
                return PayTradeResult.of(PayTradeState.UNKNOWN);
            }
            String code = result.path("code").asText("");
            if ("10000".equals(code)) {
                return switch (result.path("trade_status").asText("")) {
                    case "TRADE_SUCCESS", "TRADE_FINISHED" ->
                            new PayTradeResult(PayTradeState.SUCCESS, result.path("trade_no").asText(null));
                    case "TRADE_CLOSED" -> PayTradeResult.of(PayTradeState.CLOSED);
                    default -> PayTradeResult.of(PayTradeState.WAITING);
                };
            }
            // 订单不存在 = 平台认可了这次请求，只是还没付
            if ("40004".equals(code)) {
                return PayTradeResult.of(PayTradeState.WAITING);
            }
            log.warn("支付宝查单失败 code={} msg={} subMsg={}", code,
                    result.path("msg").asText(""), result.path("sub_msg").asText(""));
            return PayTradeResult.of(PayTradeState.UNKNOWN);
        } catch (Exception e) {
            log.warn("支付宝查单请求异常：{}", e.getMessage());
            return PayTradeResult.of(PayTradeState.UNKNOWN);
        }
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        return AlipayCrypto.verify(params, alipayPublicKey);
    }

    private static String escape(String subject) {
        return subject == null ? "" : subject.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String abbreviate(String text) {
        return text == null ? "" : text.substring(0, Math.min(text.length(), 300));
    }
}
