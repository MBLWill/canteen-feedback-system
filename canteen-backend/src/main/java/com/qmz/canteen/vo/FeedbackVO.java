package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackVO {

    private Long id;
    private Long userId;
    private String userName;
    private String avatar;
    private Long stallId;
    private String stallName;
    private String title;
    private String content;
    private String type;
    private String typeLabel;
    private String status;
    private String statusLabel;
    private String replyContent;
    private String replyUserName;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}
