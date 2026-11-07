package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.config.CommerceProperties;
import com.qmz.canteen.dto.CartAddRequest;
import com.qmz.canteen.dto.CheckoutRequest;
import com.qmz.canteen.dto.OrderItemRequest;
import com.qmz.canteen.dto.OrderRequest;
import com.qmz.canteen.entity.CartItem;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.mapper.CartItemMapper;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.OrderMapper;
import com.qmz.canteen.mapper.StallMapper;
import com.qmz.canteen.service.CartService;
import com.qmz.canteen.service.OrderService;
import com.qmz.canteen.service.PayService;
import com.qmz.canteen.vo.CartItemVO;
import com.qmz.canteen.vo.CartStallGroupVO;
import com.qmz.canteen.vo.CartVO;
import com.qmz.canteen.vo.CheckoutVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final DishMapper dishMapper;
    private final StallMapper stallMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final PayService payService;
    private final CommerceProperties props;

    @Override
    public CartVO list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdateTime));

        Map<Long, Dish> dishes = loadDishes(items);
        Map<Long, Stall> stalls = loadStalls(items);

        Map<Long, List<CartItem>> byStall = items.stream()
                .collect(Collectors.groupingBy(CartItem::getStallId, LinkedHashMap::new, Collectors.toList()));

        List<CartStallGroupVO> groups = byStall.entrySet().stream().map(entry -> {
            List<CartItemVO> vos = entry.getValue().stream()
                    .map(item -> toVO(item, dishes.get(item.getDishId()), stalls.get(entry.getKey())))
                    .toList();
            CartStallGroupVO group = new CartStallGroupVO();
            group.setStallId(entry.getKey());
            Stall stall = stalls.get(entry.getKey());
            group.setStallName(stall == null ? null : stall.getName());
            group.setWindowNo(stall == null ? null : stall.getWindowNo());
            group.setItems(vos);
            group.setTotalQuantity(vos.stream().mapToInt(CartItemVO::getQuantity).sum());
            group.setTotalAmount(props.getCheckout().money(
                    vos.stream().map(CartItemVO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
            return group;
        }).toList();

        CartVO vo = new CartVO();
        vo.setGroups(groups);
        vo.setTotalQuantity(groups.stream().mapToInt(CartStallGroupVO::getTotalQuantity).sum());
        vo.setTotalAmount(props.getCheckout().money(
                groups.stream().map(CartStallGroupVO::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        vo.setMaxQuantityPerDish(props.getCart().getMaxQuantityPerDish());
        vo.setAllowMultiStall(props.getCart().isAllowMultiStall());
        return vo;
    }

    @Override
    public void add(CartAddRequest request, Long userId) {
        Dish dish = dishMapper.selectById(request.getDishId());
        if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) {
            throw new BusinessException("菜品不存在或已下架");
        }

        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getDishId, request.getDishId()));

        int maxQty = props.getCart().getMaxQuantityPerDish();
        if (existing != null) {
            existing.setQuantity(Math.min(existing.getQuantity() + request.getQuantity(), maxQty));
            cartItemMapper.updateById(existing);
            return;
        }

        Long count = cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        if (count >= props.getCart().getMaxItemTypes()) {
            throw new BusinessException("购物车最多存放 " + props.getCart().getMaxItemTypes() + " 种菜品");
        }

        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setDishId(dish.getId());
        item.setStallId(dish.getStallId());
        item.setQuantity(Math.min(request.getQuantity(), maxQty));
        try {
            cartItemMapper.insert(item);
        } catch (DuplicateKeyException e) {
            // 连点加购时两个请求同时走到这里，uk_user_dish 冲突后按加量处理，而不是报错
            CartItem raced = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getDishId, request.getDishId()));
            if (raced == null) {
                throw e;
            }
            raced.setQuantity(Math.min(raced.getQuantity() + request.getQuantity(), maxQty));
            cartItemMapper.updateById(raced);
        }
    }

    @Override
    public void updateQuantity(Long itemId, Integer quantity, Long userId) {
        CartItem item = ownedItem(itemId, userId);
        item.setQuantity(Math.min(quantity, props.getCart().getMaxQuantityPerDish()));
        cartItemMapper.updateById(item);
    }

    @Override
    public void remove(Long itemId, Long userId) {
        ownedItem(itemId, userId);
        cartItemMapper.deleteById(itemId);
    }

    @Override
    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    @Override
    @Transactional
    public CheckoutVO checkout(CheckoutRequest request, Long userId) {
        List<CartItem> all = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        if (all.isEmpty()) {
            throw new BusinessException("购物车是空的");
        }

        Long chosen = request.getStallId();
        if (chosen == null) {
            List<Long> stallIds = all.stream().map(CartItem::getStallId).distinct().toList();
            if (stallIds.size() > 1) {
                throw new BusinessException("购物车包含多个档口的菜品，请选择档口分别结算");
            }
            chosen = stallIds.get(0);
        }
        final Long stallId = chosen;

        List<Long> pickedIds = request.getItemIds();
        List<CartItem> picked = all.stream()
                .filter(i -> i.getStallId().equals(stallId))
                .filter(i -> pickedIds == null || pickedIds.isEmpty() || pickedIds.contains(i.getId()))
                .toList();
        if (picked.isEmpty()) {
            throw new BusinessException("该档口没有勾选任何菜品");
        }

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setStallId(stallId);
        orderRequest.setPickupTime(request.getPickupTime());
        orderRequest.setRemark(request.getRemark());
        orderRequest.setItems(picked.stream().map(item -> {
            OrderItemRequest ir = new OrderItemRequest();
            ir.setDishId(item.getDishId());
            ir.setQuantity(item.getQuantity());
            return ir;
        }).toList());

        Long orderId = orderService.createOrder(orderRequest, userId);

        DiningOrder order = orderMapper.selectById(orderId);
        BigDecimal subtotal = order.getTotalAmount();
        CommerceProperties.Checkout checkout = props.getCheckout();
        if (checkout.getMinOrderAmount().signum() > 0 && subtotal.compareTo(checkout.getMinOrderAmount()) < 0) {
            throw new BusinessException("满 " + checkout.getMinOrderAmount() + " 元才能结算");
        }

        BigDecimal fee = checkout.getPackingFee();
        BigDecimal delivery = checkout.getDeliveryFee();
        boolean freeDelivery = checkout.getFreeDeliveryThreshold().signum() > 0
                && subtotal.compareTo(checkout.getFreeDeliveryThreshold()) >= 0;
        if (delivery.signum() > 0 && !freeDelivery) {
            fee = fee.add(delivery);
        }
        BigDecimal total = checkout.money(subtotal.add(fee));

        DiningOrder update = new DiningOrder();
        update.setId(orderId);
        update.setTotalAmount(total);
        update.setExpireTime(LocalDateTime.now().plusMinutes(props.getOrder().getPayTimeoutMinutes()));
        orderMapper.updateById(update);
        order.setTotalAmount(total);
        order.setExpireTime(update.getExpireTime());

        CheckoutVO vo = payService.createForOrder(orderId, userId);

        // 只清掉本次结算的条目，未勾选的留在购物车
        cartItemMapper.deleteBatchIds(picked.stream().map(CartItem::getId).toList());
        return vo;
    }

    private CartItem ownedItem(Long itemId, Long userId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("购物车条目不存在");
        }
        return item;
    }

    private CartItemVO toVO(CartItem item, Dish dish, Stall stall) {
        CartItemVO vo = new CartItemVO();
        vo.setId(item.getId());
        vo.setDishId(item.getDishId());
        vo.setQuantity(item.getQuantity());
        if (dish != null) {
            vo.setDishName(dish.getName());
            vo.setImage(dish.getImage());
            vo.setCategory(dish.getCategory());
            vo.setPrice(dish.getPrice());
            vo.setDishStatus(dish.getStatus());
            vo.setAmount(props.getCheckout().money(
                    dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
        }
        vo.setStallId(item.getStallId());
        if (stall != null) {
            vo.setStallName(stall.getName());
            vo.setWindowNo(stall.getWindowNo());
        }
        return vo;
    }

    private Map<Long, Dish> loadDishes(List<CartItem> items) {
        List<Long> ids = items.stream().map(CartItem::getDishId).distinct().toList();
        return ids.isEmpty() ? Map.of() : dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                        .in(Dish::getId, ids)).stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity()));
    }

    private Map<Long, Stall> loadStalls(List<CartItem> items) {
        List<Long> ids = items.stream().map(CartItem::getStallId).distinct().toList();
        return ids.isEmpty() ? Map.of() : stallMapper.selectList(new LambdaQueryWrapper<Stall>()
                        .in(Stall::getId, ids)).stream()
                .collect(Collectors.toMap(Stall::getId, Function.identity()));
    }
}
