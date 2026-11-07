package com.qmz.canteen.service;

import com.qmz.canteen.vo.DishVO;

import java.util.List;

public interface FavoriteService {

    boolean add(Long dishId, Long userId);

    boolean remove(Long dishId, Long userId);

    List<DishVO> listMyFavorites(Long userId);
}
