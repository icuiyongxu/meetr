package com.meetr.application;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.meetr.domain.entity.Booking;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.NotificationEventType;
import com.meetr.domain.event.BookingDomainEvent;
import com.meetr.mapper.BookingMapper;
import com.meetr.mapper.BookingOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 会议提醒任务。
 *
 * 每分钟执行一次，扫描即将开始的预约，
 * 向预约人及参会人发送 BOOKING_REMINDER 站内通知。
 *
 * 通过 booking_operation_log 避免重复提醒（同一条预约在提醒周期内只提醒一次）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderTask {

    private final BookingMapper bookingMapper;
    private final BookingOperationLogMapper bookingOperationLogMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** 提前多少分钟发提醒（默认 15 分钟） */
    private static final int REMIND_BEFORE_MINUTES = 15;

    /** 同一个预约在多少分钟内不重复提醒（默认 30 分钟，防止重复触发） */
    private static final int DEDUP_MINUTES = 30;

    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    public void sendReminders() {
        try {
            doSendReminders();
        } catch (Exception e) {
            log.error("会议提醒任务执行异常: {}", e.getMessage(), e);
        }
    }

    private void doSendReminders() {
        long now = System.currentTimeMillis();
        long remindAt = now + REMIND_BEFORE_MINUTES * 60_000L;
        long windowStart = now - DEDUP_MINUTES * 60_000L;

        // 查询即将开始的预约（当前时间 ~REMIND_BEFORE_MINUTES 分钟后开始，且未开始）
        PageHelper.startPage(0, 200);
        List<Booking> upcoming = bookingMapper.findUpcomingBookings(now, remindAt);
        if (upcoming.isEmpty()) {
            return;
        }

        // 已发过提醒的 bookingId（dedup）
        Set<Long> alreadyReminded = new HashSet<>(
            bookingOperationLogMapper.findRemindedBookingIdsInWindow(
                windowStart, "BOOKING_REMINDER", now));

        int sent = 0;
        for (Booking booking : upcoming) {
            if (alreadyReminded.contains(booking.getId())) {
                continue;
            }
            if (booking.getStatus() == BookingStatus.CANCELED) {
                continue;
            }
            if (booking.getApprovalStatus() != ApprovalStatus.APPROVED
                && booking.getApprovalStatus() != ApprovalStatus.NONE) {
                // 还在审批中或已驳回，不提醒
                continue;
            }

            publishReminder(booking);
            sent++;
        }

        if (sent > 0) {
            log.info("会议提醒任务完成: 发送 {} 条提醒", sent);
        }
    }

    private void publishReminder(Booking booking) {
        // 记录日志，防止重复触发
        com.meetr.domain.entity.BookingOperationLog logEntry =
            new com.meetr.domain.entity.BookingOperationLog();
        logEntry.setBookingId(booking.getId());
        logEntry.setOperationType("BOOKING_REMINDER");
        logEntry.setOperatorId("SYSTEM");
        logEntry.setOperatorName("系统提醒");
        logEntry.setContent("系统自动提醒：会议即将开始");
        logEntry.setCreatedAtMs(System.currentTimeMillis());
        logEntry.setUpdatedAtMs(System.currentTimeMillis());
        bookingOperationLogMapper.insert(logEntry);

        // 通知预约人
        java.util.List<String> targets = new java.util.ArrayList<>();
        if (booking.getBookerId() != null && !booking.getBookerId().isBlank()) {
            targets.add(booking.getBookerId());
        }
        if (!targets.isEmpty()) {
            eventPublisher.publishEvent(
                new BookingDomainEvent(this, NotificationEventType.BOOKING_REMINDER, booking, targets));
        }
    }
}
