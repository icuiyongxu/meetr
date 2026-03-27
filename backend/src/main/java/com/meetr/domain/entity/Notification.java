package com.meetr.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private String userId;
    private String eventType;
    private String title;
    private String content;
    private Long bookingId;
    private Long roomId;
    private Long bookingStartTimeMs;
    private Long bookingEndTimeMs;
    private Boolean isRead;
    private Long readAt;
    private Long createdAtMs;

    public void markAsRead() {
        this.isRead = true;
        this.readAt = System.currentTimeMillis();
    }
}
