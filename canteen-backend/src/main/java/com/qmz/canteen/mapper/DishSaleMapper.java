package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qmz.canteen.entity.DishSale;
import com.qmz.canteen.vo.SalesDishVO;
import com.qmz.canteen.vo.SalesDailyVO;
import com.qmz.canteen.vo.TrendDishVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DishSaleMapper extends BaseMapper<DishSale> {

    /** 某档口近 N 天各菜销量汇总（按销量降序） */
    List<SalesDishVO> selectDishSales(@Param("stallId") Long stallId,
                                      @Param("days") Integer days);

    /** 某档口近 N 天每日收入汇总（按日期升序） */
    List<SalesDailyVO> selectDailyIncome(@Param("stallId") Long stallId,
                                         @Param("days") Integer days);

    /** 某档口近 N 天每日销量汇总（按日期升序，含菜品明细） */
    List<SalesDailyVO> selectDailyQuantity(@Param("stallId") Long stallId,
                                           @Param("days") Integer days);

    /** 潜力菜品：近 7 天 vs 前 7 天销量对比，增长率降序 */
    List<TrendDishVO> selectTrendDishes(@Param("stallId") Long stallId);
}
