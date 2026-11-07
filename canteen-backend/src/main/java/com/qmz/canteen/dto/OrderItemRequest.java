package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "份数不能为空")
    @Min(value = 1, message = "每道菜至少1份")
    @Max(value = 20, message = "每道菜最多20份")
    private Integer quantity;
}
