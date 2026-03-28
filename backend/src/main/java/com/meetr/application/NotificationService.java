package com.meetr.application;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.entity.Notification;
import com.meetr.domain.entity.SysUser;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.enums.NotificationEventType;
import com.meetr.mapper.BookingAttendeeMapper;
import com.meetr.mapper.BookingOperationLogMapper;
import com.meetr.mapper.MeetingRoomMapper;
import com.meetr.mapper.NotificationMapper;
import com.meetr.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final BookingAttendeeMapper bookingAttendeeMapper;
    private final BookingOperationLogMapper bookingOperationLogMapper;
    private final EmailService emailService;

    /**
     * 发送预约事件通知。
     * 对每个目标用户：写入站内通知 + 判断是否发邮件。
     */
    @Transactional
    public void notify(NotificationEventType eventType, Booking booking, List<String> targetUserIds) {
        String title = buildTitle(eventType, booking);
        String content = buildContent(eventType, booking);

        for (String userId : targetUserIds) {
            // 1. 写入站内通知
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setEventType(eventType.name());
            notification.setTitle(title);
            notification.setContent(content);
            notification.setBookingId(booking.getId());
            notification.setRoomId(booking.getRoomId());
            notification.setBookingStartTimeMs(booking.getStartTimeMs());
            notification.setBookingEndTimeMs(booking.getEndTimeMs());
            notification.setCreatedAtMs(System.currentTimeMillis());
            notificationMapper.insert(notification);

            // 2. 邮件通知（用户开启了邮件开关）
            sendEmailIfEnabled(userId, title, content);
        }

        log.info("通知发送完成: eventType={}, bookingId={}, targetUsers={}",
            eventType, booking.getId(), targetUserIds.size());
    }

    /**
     * 构建通知标题。
     */
    private String buildTitle(NotificationEventType eventType, Booking booking) {
        return switch (eventType) {
            case BOOKING_CREATED  -> "预约已创建";
            case BOOKING_UPDATED  -> "预约已变更";
            case BOOKING_CANCELED  -> "预约已取消";
            case BOOKING_APPROVAL_REQUIRED -> "有新的预约待审批";
            case BOOKING_APPROVED  -> "预约已通过";
            case BOOKING_REJECTED  -> "预约已拒绝";
            case BOOKING_REMINDER  -> "会议即将开始";
        };
    }

    /**
     * 构建通知正文。
     */
    private String buildContent(NotificationEventType eventType, Booking booking) {
        MeetingRoom room = meetingRoomMapper.findById(booking.getRoomId());
        String roomName = room != null ? room.getName() : "未知会议室";
        String timeStr = formatTimeRange(booking.getStartTimeMs(), booking.getEndTimeMs());

        String base = "主题：" + booking.getSubject() + "\n" +
                      "会议室：" + roomName + "\n" +
                      "时间：" + timeStr + "\n";

        return switch (eventType) {
            case BOOKING_CREATED  -> base + "您的预约已创建成功。";
            case BOOKING_UPDATED  -> base + "预约信息已变更，请注意查看。";
            case BOOKING_CANCELED  -> base + "该预约已被取消。";
            case BOOKING_APPROVAL_REQUIRED -> base + "该预约正在等待管理员审批，请及时处理。";
            case BOOKING_APPROVED  -> base + "您的预约已通过审批。" + approvalRemarkSummary(booking.getId(), "审批通过");
            case BOOKING_REJECTED  -> base + "很遗憾，您的预约未通过审批。" + approvalRemarkSummary(booking.getId(), "审批驳回");
            case BOOKING_REMINDER  -> base + "会议即将开始，请准时参加。";
        };
    }

    private String approvalRemarkSummary(Long bookingId, String prefix) {
        if (bookingId == null) return "";
        try {
            var logs = bookingOperationLogMapper.findByBookingId(bookingId);
            if (logs == null || logs.isEmpty()) return "";
            for (int i = logs.size() - 1; i >= 0; i--) {
                var log = logs.get(i);
                if (log.getOperationType() == null) continue;
                if ("APPROVE".equals(log.getOperationType()) || "REJECT".equals(log.getOperationType())) {
                    String content = log.getContent();
                    if (content == null || content.isBlank()) return "";
                    if (content.startsWith(prefix + "：")) {
                        return "\n审批意见：" + content.substring((prefix + "：").length());
                    }
                    return "\n审批意见：" + content;
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private String formatTimeRange(Long startMs, Long endMs) {
        if (startMs == null || endMs == null) return "-";
        java.time.Instant s = java.time.Instant.ofEpochMilli(startMs);
        java.time.Instant e = java.time.Instant.ofEpochMilli(endMs);
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        java.time.LocalDateTime ls = java.time.LocalDateTime.ofInstant(s, zone);
        java.time.LocalDateTime le = java.time.LocalDateTime.ofInstant(e, zone);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm");
        return ls.format(fmt) + " - " + le.format(fmt);
    }

    /**
     * 如果用户开启了邮件通知，则发送邮件。
     */
    private void sendEmailIfEnabled(String userId, String title, String emailSubject) {
        try {
            SysUser user = sysUserMapper.findByUserIdWithEmail(userId);
            if (user == null) return;
            if (user.getEmail() == null || user.getEmail().isBlank()) return;
            if (user.getEmailEnabled() == null || !user.getEmailEnabled()) return;
            emailService.sendBookingNotification(user.getEmail(), "[Meetr] " + emailSubject,
                "您好，" + emailSubject + "。");
        } catch (Exception e) {
            log.warn("邮件发送失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 分页获取用户通知列表。
     */
    public PageInfo<Notification> getNotifications(String userId, int page, int size) {
        PageHelper.startPage(page + 1, size);
        List<Notification> list = notificationMapper.findByUserIdPaged(userId, page * size, size);
        return new PageInfo<>(list);
    }

    /**
     * 获取未读数。
     */
    public long getUnreadCount(String userId) {
        return notificationMapper.countUnread(userId);
    }

    /**
     * 标记单条已读。
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification n = notificationMapper.findById(notificationId);
        if (n != null && !Boolean.TRUE.equals(n.getIsRead())) {
            notificationMapper.markAsRead(notificationId, System.currentTimeMillis());
        }
    }

    /**
     * 全部已读。
     */
    @Transactional
    public void markAllAsRead(String userId) {
        notificationMapper.markAllAsRead(userId, System.currentTimeMillis());
    }
}
