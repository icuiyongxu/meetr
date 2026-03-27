package com.meetr.application;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.BookingAttendee;
import com.meetr.domain.event.BookingDomainEvent;
import com.meetr.domain.enums.NotificationEventType;
import com.meetr.mapper.BookingAttendeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 监听预约领域事件，发送站内通知和邮件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final BookingAttendeeMapper bookingAttendeeMapper;

    @Async
    @EventListener
    public void onBookingEvent(BookingDomainEvent event) {
        try {
            Booking booking = event.getBooking();
            List<String> targetUserIds = resolveTargetUsers(booking, event.getEventType());

            if (targetUserIds.isEmpty()) {
                log.debug("没有需要通知的用户: eventType={}, bookingId={}",
                    event.getEventType(), booking.getId());
                return;
            }

            notificationService.notify(event.getEventType(), booking, targetUserIds);

        } catch (Exception e) {
            log.error("发送通知失败: eventType={}, bookingId={}, error={}",
                event.getEventType(), event.getBooking().getId(), e.getMessage(), e);
        }
    }

    /**
     * 根据事件类型和预约确定需要通知的用户列表。
     * - BOOKING_CREATED / UPDATED / CANCELED：预约人 + 参会人
     * - BOOKING_APPROVED / REJECTED：仅预约人
     * - BOOKING_REMINDER：预约人 + 参会人
     */
    private List<String> resolveTargetUsers(Booking booking, NotificationEventType eventType) {
        Set<String> users = new HashSet<>();

        // 预约人始终收到通知
        if (booking.getBookerId() != null) {
            users.add(booking.getBookerId());
        }

        // 参会人也收到通知（CREATED / UPDATED / CANCELED / REMINDER）
        if (eventType != NotificationEventType.BOOKING_APPROVED
            && eventType != NotificationEventType.BOOKING_REJECTED) {
            List<BookingAttendee> attendees = bookingAttendeeMapper.findByBookingIdOrderByIdAsc(booking.getId());
            for (BookingAttendee a : attendees) {
                if (a.getUserId() != null && !a.getUserId().isBlank()) {
                    users.add(a.getUserId());
                }
            }
        }

        return new ArrayList<>(users);
    }
}
