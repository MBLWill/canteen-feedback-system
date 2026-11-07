package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.DishQuery;
import com.qmz.canteen.dto.DishRequest;
import com.qmz.canteen.vo.DishDetailVO;
import com.qmz.canteen.vo.DishVO;

import java.util.List;

public interface DishService {

    PageResult<DishVO> pageDishes(DishQuery query, Long userId);

    DishDetailVO getDishDetail(Long dishId, Long userId);

    /** operatorStallId 非 null 表示商家操作，只能管自己档口；null 表示管理员 */
    Long createDish(DishRequest request, Long operatorStallId);

    void updateDish(Long id, DishRequest request, Long operatorStallId);

    void deleteDish(Long id, Long operatorStallId);

    List<String> listCategories();
}
