package com.meetr.domain.service;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.entity.RoomConfig;
import com.meetr.domain.repository.BookingRepository;
import com.meetr.domain.vo.RuleViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingRuleService {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final BookingRepository bookingRepository;

    public List<RuleViolation> validate(Booking booking, MeetingRoom room, RoomConfig config) {
        List<RuleViolation> violations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (booking.getStartTime().isBefore(now)) {
            violations.add(new RuleViolation("R1", "不能预约过去的时间"));
        }

        if (!booking.getStartTime().isBefore(booking.getEndTime())) {
            violations.add(new RuleViolation("R2", "开始时间必须早于结束时间"));
        }

        long durationMinutes = booking.timeSlot().durationMinutes();
        if (durationMinutes > config.getMaxDurationMinutes()) {
            violations.add(new RuleViolation("R4", "单次预约时长不能超过" + config.getMaxDurationMinutes() + "分钟"));
        }

        if (durationMinutes < config.getMinDurationMinutes()) {
            violations.add(new RuleViolation("R5", "单次预约时长不能少于" + config.getMinDurationMinutes() + "分钟"));
        }

        long minutesAhead = ChronoUnit.MINUTES.between(now, booking.getStartTime());
        if (minutesAhead < config.getMinBookAheadMinutes()) {
            violations.add(new RuleViolation("R6", "至少需要提前" + config.getMinBookAheadMinutes() + "分钟预约"));
        }

        long daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), booking.getStartTime().toLocalDate());
        if (daysAhead > config.getMaxBookAheadDays()) {
            violations.add(new RuleViolation("R7", "最多只能提前" + config.getMaxBookAheadDays() + "天预约"));
        }

        LocalDate date = booking.getStartTime().toLocalDate(BEIJING);
        long dayStartMs = date.atStartOfDay(BEIJING).toInstant().toEpochMilli();
        long dayEndMs = date.plusDays(1).atStartOfDay(BEIJING).toInstant().toEpochMilli();
        long dayBookings = bookingRepository.countActiveBookingsOnDay(
            booking.getBookerId(),
            dayStartMs,
            dayEndMs,
            booking.getId()
        );
        if (dayBookings >= config.getMaxPerDay()) {
            violations.add(new RuleViolation("R8", "同一天最多预约" + config.getMaxPerDay() + "次"));
        }

        if (booking.getAttendeeCount() != null && room.getCapacity() != null
            && booking.getAttendeeCount() > room.getCapacity()) {
            violations.add(new RuleViolation("ROOM_CAPACITY", "参会人数不能超过会议室容量"));
        }

        return violations;
    }
}
