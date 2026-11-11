package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_REPLY = "REPLY";
    public static final String TYPE_MERCHANT_REPLY = "MERCHANT_REPLY";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者用户ID */
    private Long userId;

    /** LIKE=点赞 REPLY=学生回复 MERCHANT_REPLY=商家回复 */
    private String type;

    /** 关联评价ID */
    private Long reviewId;

    /** 关联菜品ID */
    private Long dishId;

    /** 关联回复ID */
    private Long replyId;

    /** 通知内容摘要 */
    private String content;

    /** 0未读 1已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}
