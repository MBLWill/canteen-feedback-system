package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.NotificationService;
import com.qmz.canteen.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 消息列表（时间倒序） */
    @GetMapping
    public Result<List<NotificationVO>> list(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(notificationService.list(UserContext.getUserId(), limit));
    }

    /** 未读消息数 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        long count = notificationService.countUnread(UserContext.getUserId());
        return Result.success(Map.of("count", count));
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        notificationService.markAllRead(UserContext.getUserId());
        return Result.success();
    }
}
