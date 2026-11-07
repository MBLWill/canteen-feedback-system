package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatLogVO {

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String question;
    private String intent;
    private String intentLabel;
    private Integer hit;
    private Integer costMs;
    private Integer helpful;
    private LocalDateTime createTime;
}
