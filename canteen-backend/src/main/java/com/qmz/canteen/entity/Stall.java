package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canteen_stall")
public class Stall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String windowNo;

    /** 食堂区域：south-南区 north-北区 */
    private String area;

    /** 楼层：1/2，北区固定 1 */
    private Integer floor;

    private String location;

    private String category;

    private String managerName;

    private String managerPhone;

    private String coverImage;

    private String description;

    /** 学校评定的卫生分，0-100 */
    private java.math.BigDecimal hygieneScore;

    /** 健康证图片 */
    private String healthCertUrl;

    /** 营业执照图片 */
    private String licenseUrl;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
