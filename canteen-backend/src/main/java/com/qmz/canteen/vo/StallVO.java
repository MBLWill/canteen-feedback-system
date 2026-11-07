package com.qmz.canteen.vo;

import lombok.Data;

import java.util.List;

@Data
public class StallVO {

    private Long id;
    private String name;
    private String windowNo;
    /** 食堂区域：south-南区 north-北区 */
    private String area;
    /** 楼层：1/2 */
    private Integer floor;
    /** 商家登录账号，无商家账号时为 null */
    private String merchantUsername;
    private String location;
    private String category;
    private String managerName;
    private String managerPhone;
    private String coverImage;
    private String description;
    private Integer status;
    /** 学校评定的卫生分，低于 80 需在档口卡片公示整改 */
    private java.math.BigDecimal hygieneScore;
    private Long dishCount;
    private Double avgRating;
    private Long reviewCount;
    private Double goodRate;
    private Long favoriteCount;
    /** 累计销量（销售记录 + 已支付订单） */
    private Long salesCount;
    /** 招牌菜，优先取 featured=1 的菜品，不足时按评分补齐 */
    private List<DishVO> signatureDishes;
}
