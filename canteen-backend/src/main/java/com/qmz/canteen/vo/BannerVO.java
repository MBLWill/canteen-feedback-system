package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BannerVO {

    private Long id;
    private Long stallId;
    private String stallName;
    private String title;
    private String subtitle;
    private String image;
    private String bgColor;
    private String emoji;
    private String link;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createTime;
}
