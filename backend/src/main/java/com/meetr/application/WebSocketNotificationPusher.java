package com.meetr.application;

import com.meetr.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 通过 WebSocket 实时推送通知给前端。
 * 前端订阅 /user/queue/notifications 即可收到。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationPusher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送单条通知给指定用户。
     */
    public void push(String userId, Notification notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification
            );
            log.debug("WebSocket推送成功: userId={}, notificationId={}", userId, notification.getId());
        } catch (Exception e) {
            log.warn("WebSocket推送失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
