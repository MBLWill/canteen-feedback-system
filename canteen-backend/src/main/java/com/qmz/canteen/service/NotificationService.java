package com.qmz.canteen.service;

import com.qmz.canteen.vo.NotificationVO;

import java.util.List;

public interface NotificationService {

    List<NotificationVO> list(Long userId, int limit);

    long countUnread(Long userId);

    void markAllRead(Long userId);
}
