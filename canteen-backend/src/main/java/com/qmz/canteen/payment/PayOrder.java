package com.qmz.canteen.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 发起支付时网关需要的最小信息 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOrder {

    private String payNo;
    private BigDecimal amount;
    private String subject;
    private String notifyUrl;
    private String returnUrl;
}
