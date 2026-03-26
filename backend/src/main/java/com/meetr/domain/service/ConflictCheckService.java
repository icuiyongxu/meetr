package com.meetr.domain.service;

import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.RoomConfig;
import com.meetr.mapper.BookingMapper;
import com.meetr.domain.vo.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConflictCheckService {

    /** 北京时区 */
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    /** UTC */
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private final BookingMapper bookingMapper;

    /**
     * 检查时间冲突，参数和返回值统一用 LocalDateTime（北京时间），
     * 内部转为 UTC 毫秒与数据库比较。
     */
    public ConflictResult hasConflict(Long roomId, TimeSlot slot, Long excludeBookingId) {
        // 把 LocalDateTime 当作北京时间，转为 UTC 毫秒
        long newStartMs = toUtcMillis(slot.start());
        long newEndMs = toUtcMillis(slot.end());
        List<Booking> conflicts = bookingMapper.findConflicting(roomId, newStartMs, newEndMs, excludeBookingId);
        return new ConflictResult(!conflicts.isEmpty(), conflicts);
    }

    /**
     * 把输入的 LocalDateTime（前端传来的北京时间字符串解析结果）
     * 对齐到 resolution 边界，返回的 TimeSlot 仍是北京时间 LocalDateTime。
     */
    public TimeSlot alignToSlot(TimeSlot raw, RoomConfig config) {
        long resolutionSeconds = config.getResolution().longValue();

        // 把输入的 LocalDateTime 当作北京时间，转 UTC 毫秒
        long startMs = toUtcMillis(raw.start());
        long endMs = toUtcMillis(raw.end());

        // 向上取整对齐
        long alignedStartMs = (startMs / resolutionSeconds) * resolutionSeconds;
        long alignedEndMs = ((endMs + resolutionSeconds - 1) / resolutionSeconds) * resolutionSeconds;

        // 转回北京时间 LocalDateTime
        LocalDateTime alignedStart = fromUtcMillis(alignedStartMs);
        LocalDateTime alignedEnd = fromUtcMillis(alignedEndMs);

        return new TimeSlot(alignedStart, alignedEnd);
    }

    // ── 转换工具 ───────────────────────────────────────

    /** LocalDateTime（北京时间） → UTC 毫秒 */
    private long toUtcMillis(LocalDateTime dt) {
        return dt.atZone(BEIJING).toInstant().toEpochMilli();
    }

    /** UTC 毫秒 → LocalDateTime（北京时间） */
    private LocalDateTime fromUtcMillis(long ms) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), BEIJING);
    }

    public record ConflictResult(boolean conflict, List<Booking> conflictingBookings) {
    }
}
