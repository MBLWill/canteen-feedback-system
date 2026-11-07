package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CartAddRequest {

    @NotNull(message = "菜品ID不能为空")
    @Positive(message = "菜品ID不合法")
    private Long dishId;

    @Min(value = 1, message = "份数至少为1")
    @Max(value = 99, message = "份数过多")
    private Integer quantity = 1;
}
