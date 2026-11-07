package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.StallRequest;
import com.qmz.canteen.entity.Banner;
import com.qmz.canteen.entity.CartItem;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.entity.DiningOrderItem;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.DishFavorite;
import com.qmz.canteen.entity.DishReview;
import com.qmz.canteen.entity.DishSale;
import com.qmz.canteen.entity.Feedback;
import com.qmz.canteen.entity.MerchantMessage;
import com.qmz.canteen.entity.MerchantTodo;
import com.qmz.canteen.entity.PayRecord;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.mapper.BannerMapper;
import com.qmz.canteen.mapper.CartItemMapper;
import com.qmz.canteen.mapper.DiningOrderItemMapper;
import com.qmz.canteen.mapper.DishFavoriteMapper;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.DishReviewMapper;
import com.qmz.canteen.mapper.DishSaleMapper;
import com.qmz.canteen.mapper.FeedbackMapper;
import com.qmz.canteen.mapper.MerchantMessageMapper;
import com.qmz.canteen.mapper.MerchantTodoMapper;
import com.qmz.canteen.mapper.OrderMapper;
import com.qmz.canteen.mapper.PayRecordMapper;
import com.qmz.canteen.mapper.StallMapper;
import com.qmz.canteen.mapper.UserMapper;
import com.qmz.canteen.security.UserRole;
import com.qmz.canteen.service.StallService;
import com.qmz.canteen.vo.StallVO;
import com.qmz.canteen.vo.StallWindowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StallServiceImpl implements StallService {

    @Value("${canteen.good-rating-threshold}")
    private Integer goodThreshold;

    private final StallMapper stallMapper;
    private final DishMapper dishMapper;
    private final BannerMapper bannerMapper;
    private final UserMapper userMapper;
    private final DishReviewMapper dishReviewMapper;
    private final DishFavoriteMapper dishFavoriteMapper;
    private final DishSaleMapper dishSaleMapper;
    private final CartItemMapper cartItemMapper;
    private final OrderMapper orderMapper;
    private final DiningOrderItemMapper orderItemMapper;
    private final PayRecordMapper payRecordMapper;
    private final FeedbackMapper feedbackMapper;
    private final MerchantMessageMapper merchantMessageMapper;
    private final MerchantTodoMapper merchantTodoMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final long SIGNATURE_LIMIT = 3L;

    @Override
    public List<StallVO> listWithStats(String keyword, String area, Integer floor) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<StallVO> stalls = stallMapper.selectStallWithStats(kw, area, floor, goodThreshold);
        stalls.forEach(this::fillSignatureDishes);
        return stalls;
    }

    @Override
    public StallVO getStallDetail(Long id) {
        StallVO stall = stallMapper.selectStallById(id, goodThreshold);
        if (stall == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "档口不存在");
        }
        fillSignatureDishes(stall);
        return stall;
    }

    private void fillSignatureDishes(StallVO stall) {
        stall.setSignatureDishes(dishMapper.selectSignatureDishes(stall.getId(), SIGNATURE_LIMIT, goodThreshold));
    }

    @Override
    public List<StallWindowVO> listWindowNos(String area, Integer floor) {
        List<Stall> stalls = stallMapper.selectList(new LambdaQueryWrapper<Stall>()
                .eq(Stall::getArea, area)
                .eq(Stall::getFloor, floor)
                .orderByAsc(Stall::getWindowNo));
        return stalls.stream().map(s -> {
            StallWindowVO vo = new StallWindowVO();
            vo.setStallId(s.getId());
            vo.setWindowNo(s.getWindowNo());
            vo.setStallName(s.getName());
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public Long createStall(StallRequest request) {
        String windowNo = request.getWindowNo().trim();
        validateWindowNo(windowNo, request.getArea(), request.getFloor());
        if (stallMapper.selectCount(new LambdaQueryWrapper<Stall>()
                .eq(Stall::getWindowNo, windowNo)) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该窗口编号已存在");
        }
        String name = request.getName().trim();
        if (stallMapper.selectCount(new LambdaQueryWrapper<Stall>()
                .eq(Stall::getName, name)) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "已存在同名档口「" + name + "」");
        }
        Stall stall = new Stall();
        BeanUtils.copyProperties(request, stall);
        // 不 trim 的话，" 03" 和 "03" 会被当成两个窗口
        stall.setWindowNo(windowNo);
        stall.setName(name);
        normalizeFloor(stall);
        stall.setStatus(stall.getStatus() == null ? 1 : stall.getStatus());
        try {
            stallMapper.insert(stall);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该窗口编号或档口名已存在");
        }
        // 商家账号随档口一起创建
        createMerchantAccount(request, stall.getId());
        return stall.getId();
    }

    private void createMerchantAccount(StallRequest request, Long stallId) {
        String username = request.getMerchantUsername() == null ? "" : request.getMerchantUsername().trim();
        String password = request.getMerchantPassword() == null ? "" : request.getMerchantPassword();
        if (username.isEmpty() || password.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请填写商家账号和初始密码");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "商家账号「" + username + "」已存在");
        }
        User merchant = new User();
        merchant.setUsername(username);
        merchant.setPassword(passwordEncoder.encode(password));
        merchant.setRealName(request.getName() + " 商家");
        merchant.setRole(UserRole.MERCHANT.name());
        merchant.setStallId(stallId);
        merchant.setStatus(1);
        try {
            userMapper.insert(merchant);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "商家账号「" + username + "」已存在");
        }
    }

    @Override
    public void updateStall(Long id, StallRequest request) {
        Stall stall = stallMapper.selectById(id);
        if (stall == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "档口不存在");
        }
        String windowNo = request.getWindowNo().trim();
        validateWindowNo(windowNo, request.getArea(), request.getFloor());
        if (stallMapper.selectCount(new LambdaQueryWrapper<Stall>()
                .eq(Stall::getWindowNo, windowNo)
                .ne(Stall::getId, id)) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该窗口编号已存在");
        }
        String name = request.getName().trim();
        if (stallMapper.selectCount(new LambdaQueryWrapper<Stall>()
                .eq(Stall::getName, name)
                .ne(Stall::getId, id)) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "已存在同名档口「" + name + "」");
        }
        BeanUtils.copyProperties(request, stall);
        stall.setWindowNo(windowNo);
        stall.setName(name);
        normalizeFloor(stall);
        try {
            stallMapper.updateById(stall);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该窗口编号或档口名已存在");
        }
    }

    /** 北区只有一层，无论前端传什么楼层都归一到 1 */
    private void normalizeFloor(Stall stall) {
        if ("north".equals(stall.getArea())) {
            stall.setFloor(1);
        }
    }

    /** 编号格式校验 + 编号前缀与区域楼层匹配校验 */
    private void validateWindowNo(String windowNo, String area, Integer floor) {
        if (!windowNo.matches("^(S[12]|N1)(0[1-9]|1[0-9]|20)$")) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "窗口编号格式不合法");
        }
        String prefix = windowNo.substring(0, 2);
        if ("south".equals(area)) {
            String expect = Integer.valueOf(1).equals(floor) ? "S1" : "S2";
            if (!expect.equals(prefix)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "窗口编号与所选区域楼层不匹配");
            }
        } else if ("north".equals(area) && !"N1".equals(prefix)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "窗口编号与所选区域楼层不匹配");
        }
    }

    @Override
    public void resetMerchantPassword(Long stallId, String password) {
        User merchant = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getStallId, stallId)
                .eq(User::getRole, UserRole.MERCHANT.name()));
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该档口还没有商家账号");
        }
        merchant.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(merchant);
    }

    @Override
    public void updateCertificates(Long stallId, String healthCertUrl, String licenseUrl) {
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "档口不存在");
        }
        Stall update = new Stall();
        update.setId(stallId);
        update.setHealthCertUrl(healthCertUrl);
        update.setLicenseUrl(licenseUrl);
        stallMapper.updateById(update);
    }

    @Override
    @Transactional
    public void deleteStall(Long id) {
        Stall stall = stallMapper.selectById(id);
        if (stall == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "档口不存在");
        }
        // 收集该档口下所有菜品 ID，用于清理评价、收藏、订单明细等以 dish_id 关联的数据
        List<Long> dishIds = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getStallId, id))
                .stream().map(Dish::getId).toList();

        // 1. 菜品评价（逻辑删除，按 dish_id）
        if (!dishIds.isEmpty()) {
            dishReviewMapper.delete(new LambdaQueryWrapper<DishReview>().in(DishReview::getDishId, dishIds));
        }
        // 2. 菜品收藏（物理删除，按 dish_id）
        if (!dishIds.isEmpty()) {
            dishFavoriteMapper.delete(new LambdaQueryWrapper<DishFavorite>().in(DishFavorite::getDishId, dishIds));
        }
        // 3. 销量记录（物理删除，按档口）
        dishSaleMapper.delete(new LambdaQueryWrapper<DishSale>().eq(DishSale::getStallId, id));

        // 4. 订单明细（逻辑删除）：先收集该档口订单，再按订单与菜品两种口径清理
        List<Long> orderIds = orderMapper.selectList(new LambdaQueryWrapper<DiningOrder>()
                        .eq(DiningOrder::getStallId, id))
                .stream().map(DiningOrder::getId).toList();
        if (!orderIds.isEmpty()) {
            orderItemMapper.delete(new LambdaQueryWrapper<DiningOrderItem>().in(DiningOrderItem::getOrderId, orderIds));
            // 5. 支付流水（物理删除，按订单）
            payRecordMapper.delete(new LambdaQueryWrapper<PayRecord>().in(PayRecord::getOrderId, orderIds));
        }
        // 订单明细里可能还存在跨档口迁移过来的菜品，按 dish_id 兜底再清一次
        if (!dishIds.isEmpty()) {
            orderItemMapper.delete(new LambdaQueryWrapper<DiningOrderItem>().in(DiningOrderItem::getDishId, dishIds));
        }
        // 6. 订单（逻辑删除，按档口）
        orderMapper.delete(new LambdaQueryWrapper<DiningOrder>().eq(DiningOrder::getStallId, id));

        // 7. 购物车（物理删除，按档口与菜品）
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getStallId, id));
        if (!dishIds.isEmpty()) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().in(CartItem::getDishId, dishIds));
        }

        // 8. 留言反馈（逻辑删除，按档口）
        feedbackMapper.delete(new LambdaQueryWrapper<Feedback>().eq(Feedback::getStallId, id));
        // 9. 商家留言（物理删除，按档口）
        merchantMessageMapper.delete(new LambdaQueryWrapper<MerchantMessage>().eq(MerchantMessage::getStallId, id));
        // 10. 商家待办（物理删除，按档口）
        merchantTodoMapper.delete(new LambdaQueryWrapper<MerchantTodo>().eq(MerchantTodo::getStallId, id));

        // 11. 菜品（逻辑删除，按档口）
        dishMapper.delete(new LambdaQueryWrapper<Dish>().eq(Dish::getStallId, id));

        // 12. 商家账号（逻辑删除，仅该档口的 MERCHANT 账号）
        userMapper.delete(new LambdaQueryWrapper<User>()
                .eq(User::getStallId, id)
                .eq(User::getRole, UserRole.MERCHANT.name()));

        // 13. 档口删除后广告还挂着 stallId，前端会显示成没有名字的入口，置空
        bannerMapper.update(null, new LambdaUpdateWrapper<Banner>()
                .eq(Banner::getStallId, id)
                .set(Banner::getStallId, null));

        // 14. 最后删除档口本身（逻辑删除）
        stallMapper.deleteById(id);
    }
}
