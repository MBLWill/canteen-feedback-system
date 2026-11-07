package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FeedbackQuery {

    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 100000, message = "页码超出范围")
    private Long current = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数最多100条")
    private Long size = 10L;

    private Long stallId;
    private String type;
    private String status;
    private String keyword;
}
