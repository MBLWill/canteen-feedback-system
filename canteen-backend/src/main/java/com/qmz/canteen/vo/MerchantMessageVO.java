package com.qmz.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantMessageVO {

    private Long id;
    private Long stallId;
    private String stallName;
    /** 商户负责人 */
    private String managerName;
    /** 窗口号 */
    private String windowNo;
    /** 商户封面/头像 */
    private String coverImage;
    /** 商家账号头像（logo），优先用于列表展示 */
    private String merchantAvatar;
    /** 区域楼层，如「南区 1 楼」 */
    private String zone;
    private String content;
    private Integer status;
    private String statusLabel;
    private String reply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}
