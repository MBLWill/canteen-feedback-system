package com.qmz.canteen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 购物车与支付配置，对应 resources/cart-payment.yml。
 * 未填写的项走这里的默认值，保证通道没配好时也能启动。
 */
@Data
@Component
@ConfigurationProperties(prefix = "canteen")
public class CommerceProperties {

    private Cart cart = new Cart();
    private Checkout checkout = new Checkout();
    private Order order = new Order();
    private Pay pay = new Pay();
    private PayJob payJob = new PayJob();

    @Data
    public static class Cart {
        private int maxItemTypes = 20;
        private int maxQuantityPerDish = 10;
        private int ttlDays = 7;
        private boolean checkStock = true;
        private boolean repriceOnCheckout = true;
        private boolean allowMultiStall = false;
    }

    @Data
    public static class Checkout {
        private BigDecimal packingFee = BigDecimal.ZERO;
        private BigDecimal deliveryFee = BigDecimal.ZERO;
        private BigDecimal freeDeliveryThreshold = BigDecimal.ZERO;
        private BigDecimal minOrderAmount = BigDecimal.ZERO;
        private int scale = 2;
        private String roundingMode = "HALF_UP";
        private String currency = "CNY";

        public RoundingMode rounding() {
            return RoundingMode.valueOf(roundingMode.toUpperCase(Locale.ROOT));
        }

        /** 统一按配置精度舍入，避免各处的 setScale 写死 */
        public BigDecimal money(BigDecimal value) {
            return (value == null ? BigDecimal.ZERO : value).setScale(scale, rounding());
        }
    }

    @Data
    public static class Order {
        private String noPrefix = "CT";
        private int payTimeoutMinutes = 15;
        private int idempotentWindowSeconds = 10;
        private boolean deductStockOnPaid = true;
    }

    @Data
    public static class Pay {
        private String channel = "";
        private boolean sandbox = true;
        private String notifyBaseUrl = "";
        private String notifyPath = "/pay/notify";
        private String returnUrl = "";
        private int queryIntervalSeconds = 2;
        private int queryMaxTimes = 30;
        private boolean verifySignature = true;
        private String notifyIpWhitelist = "";
        private boolean logRawNotify = false;
        private Mock mock = new Mock();
        private Alipay alipay = new Alipay();
        private Wechat wechat = new Wechat();

        /** 异步通知的完整地址，未配置公网地址时返回 null（只能靠主动查单） */
        public String notifyUrl() {
            if (notifyBaseUrl == null || notifyBaseUrl.isBlank()) {
                return null;
            }
            String base = notifyBaseUrl.endsWith("/")
                    ? notifyBaseUrl.substring(0, notifyBaseUrl.length() - 1) : notifyBaseUrl;
            String path = notifyPath.startsWith("/") ? notifyPath : "/" + notifyPath;
            return base + path;
        }
    }

    @Data
    public static class Mock {
        private boolean autoSuccess = true;
        private int delayMs = 800;
    }

    @Data
    public static class Alipay {
        private String appId = "";
        private String privateKey = "";
        private String alipayPublicKey = "";
        private String gateway = "";
        private String signType = "RSA2";

        public boolean configured() {
            return !appId.isBlank() && !privateKey.isBlank() && !alipayPublicKey.isBlank() && !gateway.isBlank();
        }
    }

    @Data
    public static class Wechat {
        private String appId = "";
        private String mchId = "";
        private String apiV3Key = "";
        private String serialNo = "";
        private String privateKeyPath = "";
        private String platformCertPath = "";
        private String tradeType = "NATIVE";

        public boolean configured() {
            return !appId.isBlank() && !mchId.isBlank() && !apiV3Key.isBlank();
        }
    }

    @Data
    public static class PayJob {
        private int closeTimeoutIntervalSeconds = 60;
        private String reconcileCron = "";
    }
}
