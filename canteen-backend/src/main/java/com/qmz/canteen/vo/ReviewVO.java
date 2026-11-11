package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 点赞数 */
    private Integer likeCount;
    /** 浏览量 */
    private Integer viewCount;
    /** 学生回复数 */
    private Integer replyCount;
    /** 当前用户是否已点赞 */
    private Boolean liked;
    /** 学生回复列表（详情页展示） */
    private List<ReviewReplyVO> replies;
}
