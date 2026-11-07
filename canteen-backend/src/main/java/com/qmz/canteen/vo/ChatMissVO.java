package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 未命中问题排行，用于管理员补录知识库 */
@Data
public class ChatMissVO {

    private String question;
    private Long count;
    private LocalDateTime lastTime;
}
