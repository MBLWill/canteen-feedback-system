package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OrderQuery {

    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 100000, message = "页码超出范围")
    private Long current = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数最多100条")
    private Long size = 10L;

    /** PENDING-待取餐 DONE-已完成 CANCELED-已取消 */
    private String status;

    /** UNPAID-待支付 PAID-已支付 CLOSED-已关闭 */
    private String payStatus;

    private Long stallId;

    private String keyword;
}
