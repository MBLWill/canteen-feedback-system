package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewReplyVO {

    private Long id;
    private Long reviewId;
    private Long userId;
    private String userName;
    private String avatar;
    private String content;
    private LocalDateTime createTime;
    /** 是否为当前用户发的回复 */
    private Boolean mine;
}
