package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BannerRequest {

    @NotBlank(message = "广告标题不能为空")
    @Size(max = 60, message = "广告标题不能超过60字")
    private String title;

    private Long stallId;

    @Size(max = 100, message = "副标题不能超过100字")
    private String subtitle;

    @Size(max = 255, message = "图片地址过长")
    private String image;

    @Size(max = 30, message = "背景色长度不合法")
    @Pattern(regexp = "^(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{3}|rgb\\([^)]*\\))?$",
            message = "背景色需为 #RRGGBB 或 rgb(...) 格式")
    private String bgColor;

    @Size(max = 10, message = "表情最多10个字符")
    private String emoji;

    @Size(max = 255, message = "跳转链接过长")
    private String link;

    @Min(value = 0, message = "排序号不能为负数")
    @Max(value = 9999, message = "排序号过大")
    private Integer sortNo;

    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 1, message = "状态取值不合法")
    private Integer status;
}
