package com.qmz.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantTodoRequest {

    @NotNull(message = "档口不能为空")
    @Positive(message = "档口ID不合法")
    private Long stallId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100字")
    private String title;

    @Size(max = 500, message = "说明不能超过500字")
    private String content;
}
