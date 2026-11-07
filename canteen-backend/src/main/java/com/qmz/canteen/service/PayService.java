package com.qmz.canteen.service;

import com.qmz.canteen.vo.CheckoutVO;
import com.qmz.canteen.vo.PayStatusVO;

import java.util.Map;

public interface PayService {

    /** 为订单创建支付单；mock 自动成功时返回的 payUrl 为空且 paid=true */
    CheckoutVO createForOrder(Long orderId, Long userId);

    /** 查询支付状态；CREATED 状态会先向支付平台主动查单同步 */
    PayStatusVO status(String payNo, Long userId, boolean admin);

    /** 模拟支付成功，仅 mock 通道可用 */
    void mockSuccess(String payNo, Long userId);

    /** 支付宝异步通知入口，返回给支付宝的应答文本 */
    String handleAlipayNotify(Map<String, String> params, String rawBody);

    /** 关闭超时未支付的支付单与订单 */
    void closeTimeoutOrders();
}
