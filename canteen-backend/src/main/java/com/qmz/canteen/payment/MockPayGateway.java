package com.qmz.canteen.payment;

import com.qmz.canteen.config.CommerceProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 本地模拟支付：不跳外部收银台，由前端直接确认 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockPayGateway implements PayGateway {

    private final CommerceProperties props;

    @PostConstruct
    void logState() {
        log.info("支付通道 mock 已就绪（autoSuccess={}, delay={}ms）",
                props.getPay().getMock().isAutoSuccess(), props.getPay().getMock().getDelayMs());
    }

    @Override
    public String channel() {
        return "mock";
    }

    @Override
    public boolean ready() {
        return true;
    }

    @Override
    public String payUrl(PayOrder order) {
        return null;
    }

    @Override
    public PayTradeResult query(String payNo) {
        return PayTradeResult.of(PayTradeState.WAITING);
    }

    @Override
    public boolean verifyNotify(java.util.Map<String, String> params) {
        return false;
    }
}
