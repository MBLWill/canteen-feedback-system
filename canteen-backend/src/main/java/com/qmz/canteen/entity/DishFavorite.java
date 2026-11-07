package com.qmz.canteen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dish_favorite")
public class DishFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long dishId;

    private LocalDateTime createTime;
}
