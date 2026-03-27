package com.meetr.domain.event;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.enums.NotificationEventType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 预约领域事件，在 create/update/cancel 成功后发布。
 * 由 NotificationEventListener 消费，发送站内通知和邮件。
 */
@Getter
public class BookingDomainEvent extends ApplicationEvent {

    private final NotificationEventType eventType;
    private final Booking booking;
    /** 需要通知的用户 ID 列表 */
    private final List<String> targetUserIds;

    public BookingDomainEvent(Object source, NotificationEventType eventType,
                             Booking booking, List<String> targetUserIds) {
        super(source);
        this.eventType = eventType;
        this.booking = booking;
        this.targetUserIds = targetUserIds;
    }
}
