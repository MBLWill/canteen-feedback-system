package com.qmz.canteen.payment;

import java.util.Map;

public interface PayGateway {

    String channel();

    /** 密钥是否加载成功，false 时该通道不可用 */
    boolean ready();

    /**
     * 通道是否真正可用（密钥解析成功 + 与支付平台连通）。
     * 与 ready() 的区别：ready() 只校验本地密钥格式，healthy() 还要做一次真实连通性自检，
     * 网关不可用（如沙箱 SYSTEM_ERROR、密钥失效、断网）时返回 false，结算会自动降级到 mock。
     */
    default boolean healthy() {
        return ready();
    }

    /** 收银台地址；返回 null 表示无需跳转（前端直接轮询支付状态） */
    String payUrl(PayOrder order);

    PayTradeResult query(String payNo);

    /** 异步通知验签 */
    boolean verifyNotify(Map<String, String> params);
}
