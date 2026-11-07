package com.qmz.canteen.vo;

import lombok.Data;

/** 客服答案底部的可操作按钮 */
@Data
public class ChatActionVO {

    /** link-页面/面板跳转 action-前端操作 */
    private String type;

    private String label;

    private String path;

    /** 是否主按钮，前端做高亮 */
    private Boolean primary;
}
