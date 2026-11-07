package com.qmz.canteen.task;

import com.qmz.canteen.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** 定时关单：释放超时未支付的订单，避免长期占用档口取餐名额 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayTimeoutJob {

    private final PayService payService;

    @Scheduled(initialDelay = 60, fixedDelayString = "${canteen.pay-job.close-timeout-interval-seconds:60}",
            timeUnit = TimeUnit.SECONDS)
    public void closeTimeoutOrders() {
        try {
            payService.closeTimeoutOrders();
        } catch (Exception e) {
            log.warn("超时关单任务执行失败：{}", e.getMessage());
        }
    }
}
