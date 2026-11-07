package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaqQuery {

    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 100000, message = "页码超出范围")
    private Long current = 1L;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数最多100条")
    private Long size = 10L;

    @Size(max = 30, message = "分类不合法")
    private String category;

    @Size(max = 50, message = "搜索关键词过长")
    private String keyword;
}
