package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ReviewQuery {

    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 100000, message = "页码超出范围")
    private Long current = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数最多100条")
    private Long size = 10L;

    private Long dishId;
    private Long stallId;
    private Long userId;
    private Integer rating;
    private Integer handled;
    private Integer status;
    private String keyword;
    /** 排序：time_desc=最新(默认) rating_desc=好评优先 rating_asc=差评优先 */
    private String sort;
}
