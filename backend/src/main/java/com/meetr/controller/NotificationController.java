package com.meetr.controller;

import com.github.pagehelper.PageInfo;
import com.meetr.controller.ApiResponse;
import com.meetr.application.NotificationService;
import com.meetr.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页获取通知列表。
     */
    @GetMapping
    public ApiResponse<PageInfo<Notification>> list(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(notificationService.getNotifications(userId, page, size));
    }

    /**
     * 未读数。
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@RequestParam String userId) {
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    /**
     * 标记单条已读。
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok(null);
    }

    /**
     * 全部已读。
     */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@RequestParam String userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok(null);
    }
}
