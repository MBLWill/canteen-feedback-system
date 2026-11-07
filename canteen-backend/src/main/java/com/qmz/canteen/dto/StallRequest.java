package com.qmz.canteen.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StallRequest {

    @NotBlank(message = "档口名称不能为空")
    @Size(max = 50, message = "档口名称不能超过50字")
    private String name;

    @NotBlank(message = "窗口编号不能为空")
    @Size(max = 20, message = "窗口编号不能超过20字")
    private String windowNo;

    @NotBlank(message = "食堂区域不能为空")
    @Pattern(regexp = "^(south|north)$", message = "食堂区域只能为南区或北区")
    private String area;

    @NotNull(message = "楼层不能为空")
    @Min(value = 1, message = "楼层最小为 1")
    @Max(value = 2, message = "楼层最大为 2")
    private Integer floor;

    @Size(max = 100, message = "位置不能超过100字")
    private String location;

    @Size(max = 50, message = "商家账号不能超过50字")
    private String merchantUsername;

    @Size(max = 100, message = "商家账号密码不能超过100字")
    private String merchantPassword;

    @Size(max = 30, message = "主营分类不能超过30字")
    private String category;

    @Size(max = 50, message = "负责人姓名不能超过50字")
    private String managerName;

    @Pattern(regexp = "^(1[3-9]\\d{9}|0\\d{2,3}-?\\d{7,8})?$", message = "负责人联系电话格式不正确")
    private String managerPhone;

    @Size(max = 255, message = "封面地址过长")
    private String coverImage;

    @Size(max = 500, message = "档口介绍不能超过500字")
    private String description;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "卫生分不能小于0")
    @jakarta.validation.constraints.DecimalMax(value = "100.0", message = "卫生分不能大于100")
    @jakarta.validation.constraints.Digits(integer = 3, fraction = 1, message = "卫生分最多保留1位小数")
    private java.math.BigDecimal hygieneScore;

    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 1, message = "状态取值不合法")
    private Integer status;
}
