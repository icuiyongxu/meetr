package com.meetr.controller;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.enums.RecurrenceType;
import com.meetr.mapper.BookingMapper;
import com.meetr.mapper.MeetingRoomMapper;
import com.meetr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * iCal 日历订阅接口。
 * URL 格式：/api/users/{userId}/ical?token=xxx
 * 无需登录，只需提供正确的 userId + token（与该用户的 calendarToken 匹配）。
 */
@RestController
@RequiredArgsConstructor
public class CalendarSubscriptionController {

    private static final String PRODID = "-//Meetr//Meeting Room Booking System//CN";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final BookingMapper bookingMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final AuthService authService;

    @GetMapping(value = "/api/users/{userId}/ical", produces = "text/calendar; charset=utf-8")
    public String icalFeed(@PathVariable String userId, @RequestParam String token) {
        String expectedToken = authService.getOrCreateCalendarToken(userId);
        if (!expectedToken.equals(token)) {
            throw new RuntimeException("Invalid calendar token");
        }

        long now = System.currentTimeMillis();
        long rangeEnd = now + (90L * 24 * 60 * 60 * 1000);

        List<Booking> bookings = bookingMapper.findByBookerIdAndTimeRange(userId, now, rangeEnd);

        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n");
        ical.append("VERSION:2.0\r\n");
        ical.append("PRODID:").append(PRODID).append("\r\n");
        ical.append("CALSCALE:GREGORIAN\r\n");
        ical.append("METHOD:PUBLISH\r\n");
        ical.append("X-WR-CALNAME:我的会议预约\r\n");

        for (Booking b : bookings) {
            if (b.getStatus() == BookingStatus.CANCELED) {
                continue;
            }
            String roomName = getRoomName(b.getRoomId());

            String uid = "booking-" + b.getId() + "@meetr";
            String dtstamp = utcNow();
            String dtstart = formatUtc(b.getStartTimeMs());
            String dtend = formatUtc(b.getEndTimeMs());
            String summary = escapeICal(b.getSubject());
            String description = buildDescription(b);

            ical.append("BEGIN:VEVENT\r\n");
            ical.append("UID:").append(uid).append("\r\n");
            ical.append("DTSTAMP:").append(dtstamp).append("\r\n");
            ical.append("DTSTART:").append(dtstart).append("\r\n");
            ical.append("DTEND:").append(dtend).append("\r\n");
            ical.append("SUMMARY:").append(summary).append("\r\n");
            if (roomName != null && !roomName.isEmpty()) {
                ical.append("LOCATION:").append(escapeICal(roomName)).append("\r\n");
            }
            if (!description.isEmpty()) {
                ical.append("DESCRIPTION:").append(description).append("\r\n");
            }
            if (b.getRecurrenceType() != null && b.getRecurrenceType() != RecurrenceType.NONE) {
                String rrule = buildRRule(b);
                if (rrule != null) {
                    ical.append(rrule);
                }
            }
            ical.append("STATUS:").append(b.getApprovalStatus() == ApprovalStatus.PENDING ? "TENTATIVE" : "CONFIRMED").append("\r\n");
            ical.append("END:VEVENT\r\n");
        }

        ical.append("END:VCALENDAR\r\n");
        return ical.toString();
    }

    private String getRoomName(Long roomId) {
        if (roomId == null) return null;
        var room = meetingRoomMapper.findById(roomId);
        return room != null ? room.getName() : null;
    }

    private String buildDescription(Booking b) {
        StringBuilder sb = new StringBuilder();
        if (b.getBookerName() != null) {
            sb.append("预约人：").append(b.getBookerName());
        }
        if (b.getAttendeeCount() != null && b.getAttendeeCount() > 0) {
            if (sb.length() > 0) sb.append("\\n");
            sb.append("参会人数：").append(b.getAttendeeCount()).append("人");
        }
        if (b.getRemark() != null && !b.getRemark().isBlank()) {
            if (sb.length() > 0) sb.append("\\n");
            sb.append("备注：").append(b.getRemark());
        }
        return escapeICal(sb.toString());
    }

    private String buildRRule(Booking b) {
        if (b.getRecurrenceType() == null || b.getRecurrenceType() == RecurrenceType.NONE) {
            return null;
        }
        String freq;
        switch (b.getRecurrenceType()) {
            case DAILY -> freq = "DAILY";
            case WEEKLY -> freq = "WEEKLY";
            case MONTHLY -> freq = "MONTHLY";
            case WORKDAY -> freq = "WEEKLY;BYDAY=MO,TU,WE,TH,FR";
            default -> freq = null;
        }
        if (freq == null) return null;
        StringBuilder rrule = new StringBuilder("RRULE:FREQ=").append(freq);
        if (b.getRecurrenceEndDate() != null) {
            rrule.append(";UNTIL=").append(b.getRecurrenceEndDate().toString().replace("-", "")).append("T235959Z");
        }
        rrule.append("\r\n");
        return rrule.toString();
    }

    private String formatUtc(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(BUSINESS_ZONE).format(UTC_FORMAT);
    }

    private String utcNow() {
        return Instant.now().atZone(java.time.ZoneOffset.UTC).format(UTC_FORMAT);
    }

    private String escapeICal(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                 .replace(",", "\\,")
                 .replace(";", "\\;")
                 .replace("\n", "\\n")
                 .replace("\r", "");
    }
}
