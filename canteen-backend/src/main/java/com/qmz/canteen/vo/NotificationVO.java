package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private String type;
    private Long reviewId;
    private Long dishId;
    private Long replyId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}
