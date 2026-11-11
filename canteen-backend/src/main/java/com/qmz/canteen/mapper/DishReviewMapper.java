package com.qmz.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qmz.canteen.dto.ReviewQuery;
import com.qmz.canteen.entity.DishReview;
import com.qmz.canteen.vo.ReviewVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface DishReviewMapper extends BaseMapper<DishReview> {

    IPage<ReviewVO> selectReviewPage(IPage<?> page, @Param("query") ReviewQuery query,
                                     @Param("currentUserId") Long currentUserId);

    /** 商家端：分页查询本档口菜品的学生评价 */
    IPage<ReviewVO> selectMerchantReviewPage(IPage<?> page, @Param("query") ReviewQuery query,
                                             @Param("stallId") Long stallId);

    /** 按评价ID + 档口ID 校验收属（评价对应菜品必须属于该档口） */
    @Select("SELECT r.* FROM dish_review r "
            + "INNER JOIN dish d ON r.dish_id = d.id AND d.deleted = 0 "
            + "WHERE r.id = #{reviewId} AND d.stall_id = #{stallId} AND r.deleted = 0 LIMIT 1")
    DishReview selectByIdAndStallId(@Param("reviewId") Long reviewId, @Param("stallId") Long stallId);

    /**
     * 查出该用户对该菜品的评价，包含已逻辑删除的记录。
     * dish_review 上 (dish_id, user_id) 是唯一键，逻辑删除的记录仍占位，
     * 只查未删除的会导致再次评价时 insert 撞唯一键。
     */
    @Select("SELECT * FROM dish_review WHERE dish_id = #{dishId} AND user_id = #{userId} LIMIT 1")
    DishReview selectOneIncludeDeleted(@Param("dishId") Long dishId, @Param("userId") Long userId);

    /** 更新并把逻辑删除标记复位为 0（复活历史评价） */
    @Update("UPDATE dish_review SET rating = #{rating}, content = #{content}, anonymous = #{anonymous}, "
            + "status = #{status}, deleted = 0, create_time = NOW() WHERE id = #{id}")
    int restoreAndUpdate(DishReview review);

    /** 点赞数 +1（原子） */
    @Update("UPDATE dish_review SET like_count = like_count + 1 WHERE id = #{id}")
    int incrLike(@Param("id") Long id);

    /** 点赞数 -1（原子，下限 0） */
    @Update("UPDATE dish_review SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrLike(@Param("id") Long id);

    /** 学生回复数 +1（原子） */
    @Update("UPDATE dish_review SET reply_count = reply_count + 1 WHERE id = #{id}")
    int incrReply(@Param("id") Long id);

    /** 某菜品全部已发布评价浏览量 +1（进入详情页时批量） */
    @Update("UPDATE dish_review SET view_count = view_count + 1 "
            + "WHERE dish_id = #{dishId} AND deleted = 0 AND status = 1")
    int incrViewByDish(@Param("dishId") Long dishId);

    /** 判断用户是否购买过该菜品（存在非取消订单项），用于「下单才能评价」校验 */
    @Select("SELECT COUNT(*) FROM dining_order_item oi "
            + "INNER JOIN dining_order o ON o.id = oi.order_id AND o.deleted = 0 "
            + "WHERE oi.dish_id = #{dishId} AND o.user_id = #{userId} "
            + "AND o.status <> 'CANCELED' AND oi.deleted = 0")
    int countPurchased(@Param("dishId") Long dishId, @Param("userId") Long userId);
}
