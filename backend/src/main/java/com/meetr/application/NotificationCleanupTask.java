package com.meetr.application;

import com.meetr.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每天凌晨 3 点清理 90 天前的站内通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupTask {

    private static final long RETENTION_DAYS = 90;
    private static final long RETENTION_MS = RETENTION_DAYS * 24L * 3600L * 1000L;

    private final NotificationMapper notificationMapper;

    /**
     * 每天凌晨 3:00 执行。
     * 使用固定延迟，下次任务在上次执行完成后 25 小时再触发。
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void cleanup() {
        long threshold = System.currentTimeMillis() - RETENTION_MS;
        try {
            int deleted = notificationMapper.deleteOlderThan(threshold);
            log.info("通知清理完成: 删除 {} 条 90 天前的记录, 阈值={}", deleted, threshold);
        } catch (Exception e) {
            log.error("通知清理失败: error={}", e.getMessage(), e);
        }
    }
}
