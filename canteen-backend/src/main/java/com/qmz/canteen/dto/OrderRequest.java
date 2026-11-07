package com.qmz.canteen.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "档口不能为空")
    private Long stallId;

    @NotNull(message = "请选择取餐时间")
    @Future(message = "取餐时间必须晚于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;

    @Size(max = 200, message = "备注不能超过200字")
    private String remark;

    @NotEmpty(message = "请至少选择一道菜")
    @Size(max = 20, message = "单次最多预约20个菜品")
    private List<@Valid OrderItemRequest> items;
}
