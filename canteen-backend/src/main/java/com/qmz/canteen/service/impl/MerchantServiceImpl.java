package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.MerchantTodoRequest;
import com.qmz.canteen.entity.Dish;
import com.qmz.canteen.entity.DishReview;
import com.qmz.canteen.entity.MerchantMessage;
import com.qmz.canteen.entity.MerchantTodo;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.mapper.DishMapper;
import com.qmz.canteen.mapper.DishReviewMapper;
import com.qmz.canteen.mapper.MerchantMessageMapper;
import com.qmz.canteen.mapper.MerchantTodoMapper;
import com.qmz.canteen.mapper.StallMapper;
import com.qmz.canteen.mapper.UserMapper;
import com.qmz.canteen.service.MerchantService;
import com.qmz.canteen.vo.MerchantMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    /** 差评预警阈值：评价数达标且差评占比超过该值就预警 */
    private static final int MIN_REVIEWS = 3;
    private static final int BAD_REVIEW_PERCENT = 40;
    private static final int BAD_RATING_MAX = 2;

    private final MerchantTodoMapper todoMapper;
    private final MerchantMessageMapper messageMapper;
    private final DishMapper dishMapper;
    private final DishReviewMapper dishReviewMapper;
    private final StallMapper stallMapper;
    private final UserMapper userMapper;

    @Override
    public List<MerchantTodo> listTodos(Long stallId) {
        scanBadReviews(stallId);
        return todoMapper.selectList(new LambdaQueryWrapper<MerchantTodo>()
                .eq(MerchantTodo::getStallId, stallId)
                .eq(MerchantTodo::getStatus, 0)
                .orderByDesc(MerchantTodo::getCreateTime));
    }

    @Override
    public void doneTodo(Long id, Long stallId) {
        MerchantTodo todo = todoMapper.selectById(id);
        if (todo == null || !todo.getStallId().equals(stallId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "待办不存在");
        }
        todo.setStatus(1);
        todo.setHandleTime(LocalDateTime.now());
        todoMapper.updateById(todo);
    }

    @Override
    public Long createInspectTodo(MerchantTodoRequest request) {
        if (stallMapper.selectById(request.getStallId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "档口不存在");
        }
        MerchantTodo todo = new MerchantTodo();
        todo.setStallId(request.getStallId());
        todo.setType(MerchantTodo.TYPE_INSPECT);
        todo.setTitle(request.getTitle());
        todo.setContent(request.getContent());
        todo.setStatus(0);
        todoMapper.insert(todo);
        return todo.getId();
    }

    /** 差评预警：某菜评价数达标且差评占比过高，生成一条待办（已有未处理预警则不重复） */
    private void scanBadReviews(Long stallId) {
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStallId, stallId)
                .eq(Dish::getStatus, 1));
        for (Dish dish : dishes) {
            long total = dishReviewMapper.selectCount(new LambdaQueryWrapper<DishReview>()
                    .eq(DishReview::getDishId, dish.getId())
                    .eq(DishReview::getStatus, DishReview.STATUS_PUBLISHED));
            if (total < MIN_REVIEWS) {
                continue;
            }
            long bad = dishReviewMapper.selectCount(new LambdaQueryWrapper<DishReview>()
                    .eq(DishReview::getDishId, dish.getId())
                    .eq(DishReview::getStatus, DishReview.STATUS_PUBLISHED)
                    .le(DishReview::getRating, BAD_RATING_MAX));
            if (bad * 100.0 / total < BAD_REVIEW_PERCENT) {
                continue;
            }
            boolean exists = todoMapper.selectCount(new LambdaQueryWrapper<MerchantTodo>()
                    .eq(MerchantTodo::getStallId, stallId)
                    .eq(MerchantTodo::getType, MerchantTodo.TYPE_BAD_REVIEW)
                    .eq(MerchantTodo::getDishId, dish.getId())
                    .eq(MerchantTodo::getStatus, 0)) > 0;
            if (exists) {
                continue;
            }
            MerchantTodo todo = new MerchantTodo();
            todo.setStallId(stallId);
            todo.setType(MerchantTodo.TYPE_BAD_REVIEW);
            todo.setTitle("菜品「" + dish.getName() + "」差评偏多");
            todo.setContent("近 " + total + " 条评价中 " + bad + " 条为差评（" + (bad * 100 / total) + "%），请关注口味或出品。");
            todo.setDishId(dish.getId());
            todo.setStatus(0);
            todoMapper.insert(todo);
        }
    }

    @Override
    public void sendMessage(Long userId, Long stallId, String content) {
        MerchantMessage message = new MerchantMessage();
        message.setUserId(userId);
        message.setStallId(stallId);
        message.setContent(content.trim());
        message.setStatus(0);
        messageMapper.insert(message);
    }

    @Override
    public List<MerchantMessageVO> listMyMessages(Long stallId) {
        return messageMapper.selectList(new LambdaQueryWrapper<MerchantMessage>()
                .eq(MerchantMessage::getStallId, stallId)
                .orderByDesc(MerchantMessage::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    @Override
    public PageResult<MerchantMessageVO> pageAllMessages(Long current, Long size) {
        IPage<MerchantMessage> page = messageMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MerchantMessage>().orderByAsc(MerchantMessage::getStatus)
                        .orderByDesc(MerchantMessage::getCreateTime));
        return PageResult.of(page, this::toVO);
    }

    @Override
    public void replyMessage(Long id, String reply) {
        MerchantMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "留言不存在");
        }
        message.setReply(reply.trim());
        message.setStatus(2);
        message.setReplyTime(LocalDateTime.now());
        messageMapper.updateById(message);
    }

    @Override
    public Stall getCertificates(Long stallId) {
        return stallMapper.selectById(stallId);
    }

    private MerchantMessageVO toVO(MerchantMessage m) {
        MerchantMessageVO vo = new MerchantMessageVO();
        vo.setId(m.getId());
        vo.setStallId(m.getStallId());
        Stall stall = stallMapper.selectById(m.getStallId());
        vo.setStallName(stall == null ? "未知档口" : stall.getName());
        if (stall != null) {
            vo.setManagerName(stall.getManagerName());
            vo.setWindowNo(stall.getWindowNo());
            vo.setCoverImage(stall.getCoverImage());
            vo.setZone(formatZone(stall));
            // 优先展示商家账号头像（logo），而不是档口门面图
            User merchant = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getStallId, m.getStallId())
                    .eq(User::getRole, "MERCHANT")
                    .eq(User::getDeleted, 0)
                    .last("LIMIT 1"));
            if (merchant != null) {
                vo.setMerchantAvatar(merchant.getAvatar());
            }
        }
        vo.setContent(m.getContent());
        vo.setStatus(m.getStatus());
        vo.setStatusLabel(statusLabel(m.getStatus()));
        vo.setReply(m.getReply());
        vo.setReplyTime(m.getReplyTime());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }

    private static String formatZone(Stall stall) {
        if (stall.getArea() == null) {
            return "";
        }
        if ("north".equalsIgnoreCase(stall.getArea())) {
            return "北区";
        }
        if ("south".equalsIgnoreCase(stall.getArea())) {
            int floor = stall.getFloor() == null ? 1 : stall.getFloor();
            return "南区 " + floor + " 楼";
        }
        return stall.getArea();
    }

    private static String statusLabel(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "已读";
            case 2 -> "已回复";
            default -> "未读";
        };
    }
}
