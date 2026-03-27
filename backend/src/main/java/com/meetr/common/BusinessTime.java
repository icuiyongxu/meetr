package com.meetr.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class BusinessTime {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    public static final ZoneOffset UTC = ZoneOffset.UTC;

    private BusinessTime() {
    }

    /** epoch ms -> UTC语义 LocalDateTime（仅用于当前存量代码过渡） */
    public static LocalDateTime msToUtcLocalDateTime(long epochMs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), UTC);
    }

    /** UTC语义 LocalDateTime -> epoch ms（仅用于当前存量代码过渡） */
    public static long utcLocalDateTimeToMs(LocalDateTime dateTime) {
        return dateTime.toInstant(UTC).toEpochMilli();
    }

    public static Instant msToInstant(long epochMs) {
        return Instant.ofEpochMilli(epochMs);
    }

    public static ZonedDateTime msToBusinessZdt(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(BUSINESS_ZONE);
    }

    public static LocalDateTime msToBusinessLocalDateTime(long epochMs) {
        return msToBusinessZdt(epochMs).toLocalDateTime();
    }

    public static long businessLocalDateTimeToMs(LocalDateTime localDateTime) {
        return localDateTime.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    public static long dayStartMs(LocalDate date) {
        return date.atStartOfDay(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    public static long dayEndMs(LocalDate date) {
        return date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant().toEpochMilli();
    }
}
