package com.qmz.canteen.payment;

import java.util.Map;

public interface PayGateway {

    String channel();

    /** 密钥是否加载成功，false 时该通道不可用 */
    boolean ready();

    /** 收银台地址；返回 null 表示无需跳转（前端直接轮询支付状态） */
    String payUrl(PayOrder order);

    PayTradeResult query(String payNo);

    /** 异步通知验签 */
    boolean verifyNotify(Map<String, String> params);
}
