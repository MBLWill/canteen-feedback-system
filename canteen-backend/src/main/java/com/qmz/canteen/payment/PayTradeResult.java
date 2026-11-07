package com.qmz.canteen.payment;

/** 主动查单结果：状态 + 平台交易号（查单确认支付时拿到真实流水号） */
public record PayTradeResult(PayTradeState state, String tradeNo) {

    public static PayTradeResult of(PayTradeState state) {
        return new PayTradeResult(state, null);
    }
}
