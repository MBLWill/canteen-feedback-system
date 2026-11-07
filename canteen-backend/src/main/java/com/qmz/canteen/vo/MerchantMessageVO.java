package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantMessageVO {

    private Long id;
    private Long stallId;
    private String stallName;
    private String content;
    private Integer status;
    private String statusLabel;
    private String reply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}
