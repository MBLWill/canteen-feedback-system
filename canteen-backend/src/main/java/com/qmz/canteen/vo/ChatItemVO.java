package com.qmz.canteen.vo;

import lombok.Data;

/** 客服答案中的一张卡片（菜品 / 档口 / 榜单项） */
@Data
public class ChatItemVO {

    private Long id;

    private String title;

    private String subtitle;

    private String meta;

    private String image;

    private String badge;

    /** success-warning-danger-info */
    private String badgeType;

    private String linkLabel;

    private String link;

    /** 菜品卡片是否显示「加入购物车」按钮 */
    private Boolean canAddToCart;
}
