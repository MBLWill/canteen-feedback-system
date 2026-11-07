package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long stallId;

    private String title;

    private String subtitle;

    private String image;

    private String bgColor;

    private String emoji;

    private String link;

    private Integer sortNo;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
