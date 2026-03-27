package com.meetr.domain.service;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.RoomConfig;
import com.meetr.mapper.BookingMapper;
import com.meetr.domain.vo.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConflictCheckService {

    private final BookingMapper bookingMapper;

    /**
     * 检查时间冲突。TimeSlot 中的 LocalDateTime 语义统一为 UTC 时间点，
     * 内部直接转为 epoch milli 与数据库中的 UTC 毫秒比较。
     */
    public ConflictResult hasConflict(Long roomId, TimeSlot slot, Long excludeBookingId) {
        long newStartMs = toEpochMillis(slot.start());
        long newEndMs = toEpochMillis(slot.end());
        List<Booking> conflicts = bookingMapper.findConflicting(roomId, newStartMs, newEndMs, excludeBookingId);
        return new ConflictResult(!conflicts.isEmpty(), conflicts);
    }

    /**
     * 把输入的 UTC 时间点按 resolution 对齐，
     * 返回值仍使用 UTC 语义的 LocalDateTime，便于后续统一入库。
     */
    public TimeSlot alignToSlot(TimeSlot raw, RoomConfig config) {
        long resolutionMillis = config.getResolution().longValue() * 1000;

        long startMs = toEpochMillis(raw.start());
        long endMs = toEpochMillis(raw.end());

        // 开始时间向下取整，结束时间向上取整
        long alignedStartMs = (startMs / resolutionMillis) * resolutionMillis;
        long alignedEndMs = ((endMs + resolutionMillis - 1) / resolutionMillis) * resolutionMillis;

        LocalDateTime alignedStart = fromEpochMillis(alignedStartMs);
        LocalDateTime alignedEnd = fromEpochMillis(alignedEndMs);

        return new TimeSlot(alignedStart, alignedEnd);
    }

    private long toEpochMillis(LocalDateTime dt) {
        return dt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private LocalDateTime fromEpochMillis(long ms) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC);
    }

    public record ConflictResult(boolean conflict, List<Booking> conflictingBookings) {
    }
}
