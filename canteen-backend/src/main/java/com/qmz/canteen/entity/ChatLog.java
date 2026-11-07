package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_log")
public class ChatLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String question;

    private String intent;

    private String intentLabel;

    private Integer hit;

    private Integer costMs;

    private Integer helpful;

    private LocalDateTime createTime;
}
