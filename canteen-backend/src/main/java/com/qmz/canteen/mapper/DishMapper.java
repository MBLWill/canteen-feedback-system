package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qmz.canteen.dto.DishQuery;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.vo.DishVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DishMapper extends BaseMapper<Dish> {

    IPage<DishVO> selectDishPage(IPage<?> page,
                                 @Param("query") DishQuery query,
                                 @Param("userId") Long userId,
                                 @Param("goodThreshold") Integer goodThreshold,
                                 @Param("orderSql") String orderSql);

    DishVO selectDishDetail(@Param("dishId") Long dishId,
                            @Param("userId") Long userId,
                            @Param("goodThreshold") Integer goodThreshold);

    List<DishVO> selectRecommendDishes(@Param("dishId") Long dishId,
                                       @Param("limit") Long limit,
                                       @Param("goodThreshold") Integer goodThreshold);

    List<DishVO> selectFavoriteDishes(@Param("userId") Long userId,
                                      @Param("goodThreshold") Integer goodThreshold);

    List<DishVO> selectSignatureDishes(@Param("stallId") Long stallId,
                                       @Param("limit") Long limit,
                                       @Param("goodThreshold") Integer goodThreshold);

    /** 某档口评分最高的若干菜品（美食探索档口排序页，右侧展示用） */
    List<DishVO> selectTopRatedDishes(@Param("stallId") Long stallId,
                                      @Param("limit") Long limit,
                                      @Param("goodThreshold") Integer goodThreshold);
}
