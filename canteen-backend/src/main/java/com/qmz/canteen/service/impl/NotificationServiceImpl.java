package com.qmz.canteen.service.impl;

import com.qmz.canteen.entity.Notification;
import com.qmz.canteen.mapper.NotificationMapper;
import com.qmz.canteen.service.NotificationService;
import com.qmz.canteen.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public List<NotificationVO> list(Long userId, int limit) {
        int size = limit < 1 ? 20 : Math.min(limit, 100);
        return notificationMapper.listByUser(userId, size).stream().map(this::toVO).toList();
    }

    @Override
    public long countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        vo.setReviewId(n.getReviewId());
        vo.setDishId(n.getDishId());
        vo.setReplyId(n.getReplyId());
        vo.setContent(n.getContent());
        vo.setIsRead(n.getIsRead());
        vo.setCreateTime(n.getCreateTime());
        return vo;
    }
}
