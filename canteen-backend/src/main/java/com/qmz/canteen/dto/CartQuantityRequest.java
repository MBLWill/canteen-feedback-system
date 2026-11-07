package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartQuantityRequest {

    @NotNull(message = "份数不能为空")
    @Min(value = 1, message = "份数至少为1")
    @Max(value = 99, message = "份数过多")
    private Integer quantity;
}
