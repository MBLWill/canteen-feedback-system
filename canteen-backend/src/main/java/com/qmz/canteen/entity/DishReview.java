package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dish_review")
public class DishReview {

    /** 0-待审核 1-已发布 2-已屏蔽 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_BLOCKED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dishId;

    private Long userId;

    private Integer rating;

    private String content;

    private Integer anonymous;

    private Integer status;

    /** 商家回复内容 */
    private String merchantReply;

    /** 商家回复时间 */
    private LocalDateTime merchantReplyTime;

    /** 商家处理状态：0-待处理 1-已处理 */
    private Integer handled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
