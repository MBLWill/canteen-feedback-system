package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为1-5星")
    @Max(value = 5, message = "评分范围为1-5星")
    private Integer rating;

    @Size(max = 500, message = "评价内容不能超过500字")
    private String content;

    @Min(value = 0, message = "匿名标记取值不合法")
    @Max(value = 1, message = "匿名标记取值不合法")
    private Integer anonymous;
}
