package com.qmz.canteen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CheckoutRequest {

    /** 结算哪个档口的菜；购物车只有单一档口时可留空 */
    private Long stallId;

    /** 只结算勾选的条目；为空则结算该档口全部 */
    private List<Long> itemIds;

    @NotNull(message = "取餐时间不能为空")
    private LocalDateTime pickupTime;

    @Size(max = 200, message = "备注不能超过200字")
    private String remark;
}
