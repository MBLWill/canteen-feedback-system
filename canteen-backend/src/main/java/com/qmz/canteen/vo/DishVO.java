package com.qmz.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DishVO {

    private Long id;
    private Long stallId;
    private String stallName;
    private String windowNo;
    private String name;
    private BigDecimal price;
    private String category;
    private String image;
    private String description;
    private Integer featured;
    private Integer status;
    private Double avgRating;
    private Long reviewCount;
    private Double goodRate;
    private Boolean favorited;
    private LocalDateTime createTime;
}
