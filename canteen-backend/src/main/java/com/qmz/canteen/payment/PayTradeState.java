package com.qmz.canteen.payment;

public enum PayTradeState {

    /** 用户还没付款 */
    WAITING,
    /** 支付成功 */
    SUCCESS,
    /** 已关闭 / 已退款 */
    CLOSED,
    /** 查不到该笔交易，或查单本身失败 */
    UNKNOWN
}
