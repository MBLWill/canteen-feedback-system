package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FaqVO {

    private Long id;
    private String category;
    private String question;
    private String answer;
    private String keywords;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
