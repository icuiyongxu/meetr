package com.meetr.domain.vo;

import java.time.Duration;
import java.time.LocalDateTime;

public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end must not be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end()) && end.isAfter(other.start());
    }

    public long durationMinutes() {
        return Duration.between(start, end).toMinutes();
    }
}
