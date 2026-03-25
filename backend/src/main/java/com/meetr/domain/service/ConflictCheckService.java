package com.meetr.domain.service;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.RoomConfig;
import com.meetr.domain.repository.BookingRepository;
import com.meetr.domain.vo.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ConflictCheckService {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final BookingRepository bookingRepository;

    public ConflictResult hasConflict(Long roomId, TimeSlot slot, Long excludeBookingId) {
        List<Booking> conflicts = bookingRepository.findConflicting(roomId, slot.start(), slot.end(), excludeBookingId);
        return new ConflictResult(!conflicts.isEmpty(), conflicts);
    }

    public TimeSlot alignToSlot(TimeSlot raw, RoomConfig config) {
        long resolutionSeconds = config.getResolution().longValue();

        // 统一在 Asia/Shanghai 时区处理：
        // 1. 把输入的 LocalDateTime 当作北京时间来解读
        ZonedDateTime zStart = raw.start().atZone(BEIJING);
        ZonedDateTime zEnd = raw.end().atZone(BEIJING);

        long startEpochSec = zStart.toEpochSecond();
        long endEpochSec = zEnd.toEpochSecond();

        long alignedStartSec = (startEpochSec / resolutionSeconds) * resolutionSeconds;
        long alignedEndSec = ((endEpochSec + resolutionSeconds - 1) / resolutionSeconds) * resolutionSeconds;

        return new TimeSlot(
            LocalDateTime.ofEpochSecond(alignedStartSec, 0, BEIJING),
            LocalDateTime.ofEpochSecond(alignedEndSec, 0, BEIJING)
        );
    }

    public record ConflictResult(boolean conflict, List<Booking> conflictingBookings) {
    }
}
