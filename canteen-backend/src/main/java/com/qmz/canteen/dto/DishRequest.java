package com.qmz.canteen.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DishRequest {

    @NotNull(message = "所属档口不能为空")
    private Long stallId;

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 60, message = "菜品名称不能超过60字")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负数")
    @Digits(integer = 6, fraction = 2, message = "价格最多保留2位小数")
    private BigDecimal price;

    @Size(max = 30, message = "分类不能超过30字")
    private String category;

    @Size(max = 255, message = "图片地址过长")
    private String image;

    @Size(max = 500, message = "菜品描述不能超过500字")
    private String description;

    @Min(value = 0, message = "招牌标记取值不合法")
    @Max(value = 1, message = "招牌标记取值不合法")
    private Integer featured;

    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 1, message = "状态取值不合法")
    private Integer status;
}
