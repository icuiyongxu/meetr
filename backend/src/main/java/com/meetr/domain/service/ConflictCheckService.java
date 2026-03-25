package com.meetr.domain.service;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.RoomConfig;
import com.meetr.domain.repository.BookingRepository;
import com.meetr.domain.vo.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConflictCheckService {

    private final BookingRepository bookingRepository;

    public ConflictResult hasConflict(Long roomId, TimeSlot slot, Long excludeBookingId) {
        List<Booking> conflicts = bookingRepository.findConflicting(roomId, slot.start(), slot.end(), excludeBookingId);
        return new ConflictResult(!conflicts.isEmpty(), conflicts);
    }

    public TimeSlot alignToSlot(TimeSlot raw, RoomConfig config) {
        long resolutionSeconds = config.getResolution().longValue();
        long startSeconds = raw.start().toEpochSecond(ZoneOffset.UTC);
        long endSeconds = raw.end().toEpochSecond(ZoneOffset.UTC);
        long alignedStartSeconds = (startSeconds / resolutionSeconds) * resolutionSeconds;
        long alignedEndSeconds = ((endSeconds + resolutionSeconds - 1) / resolutionSeconds) * resolutionSeconds;
        return new TimeSlot(
            LocalDateTime.ofEpochSecond(alignedStartSeconds, 0, ZoneOffset.UTC),
            LocalDateTime.ofEpochSecond(alignedEndSeconds, 0, ZoneOffset.UTC)
        );
    }

    public record ConflictResult(boolean conflict, List<Booking> conflictingBookings) {
    }
}
