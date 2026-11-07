package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.DishFavorite;
import com.qmz.canteen.mapper.DishFavoriteMapper;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.service.FavoriteService;
import com.qmz.canteen.vo.DishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    @Value("${canteen.good-rating-threshold}")
    private Integer goodThreshold;

    private final DishFavoriteMapper dishFavoriteMapper;
    private final DishMapper dishMapper;

    @Override
    public boolean add(Long dishId, Long userId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "菜品不存在");
        }
        if (dishFavoriteMapper.selectCount(new LambdaQueryWrapper<DishFavorite>()
                .eq(DishFavorite::getUserId, userId)
                .eq(DishFavorite::getDishId, dishId)) > 0) {
            return false;
        }
        DishFavorite favorite = new DishFavorite();
        favorite.setUserId(userId);
        favorite.setDishId(dishId);
        try {
            return dishFavoriteMapper.insert(favorite) > 0;
        } catch (DuplicateKeyException e) {
            // 连点两次时两个请求会同时通过上面的判断，由 uk_user_dish 兜底
            return false;
        }
    }

    @Override
    public boolean remove(Long dishId, Long userId) {
        return dishFavoriteMapper.delete(new LambdaQueryWrapper<DishFavorite>()
                .eq(DishFavorite::getUserId, userId)
                .eq(DishFavorite::getDishId, dishId)) > 0;
    }

    @Override
    public List<DishVO> listMyFavorites(Long userId) {
        return dishMapper.selectFavoriteDishes(userId, goodThreshold);
    }
}
