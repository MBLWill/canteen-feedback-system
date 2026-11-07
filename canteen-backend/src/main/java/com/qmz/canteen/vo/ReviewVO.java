package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {

    private Long id;
    private Long dishId;
    private String dishName;
    private String dishImage;
    private Long stallId;
    private String stallName;
    private Long userId;
    private String userName;
    private String avatar;
    private Integer rating;
    private String content;
    private Integer anonymous;
    private Integer status;
    private String merchantReply;
    private LocalDateTime merchantReplyTime;
    private Integer handled;
    private Boolean mine;
    private LocalDateTime createTime;
}
