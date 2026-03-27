package com.meetr.domain.service;

import com.meetr.common.BusinessTime;
import com.meetr.domain.enums.RecurrenceType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 将一条带重复规则的预约展开为多个时间段（不含主预约自身）。
 * 主预约（第一次）由调用方单独处理。
 */
@Component
public class RecurrenceExpander {

    /** 最大实例数保护，防止 endDate 设置过大 */
    private static final int MAX_INSTANCES = 180;

    /**
     * 以业务时区（当前固定为东八区）展开所有子实例（不含第一次），返回每次的 startMs/endMs。
     */
    public List<long[]> expand(LocalDateTime firstStart, LocalDateTime firstEnd,
                               RecurrenceType type, LocalDate endDate) {
        List<long[]> result = new ArrayList<>();
        if (type == null || type == RecurrenceType.NONE || endDate == null) {
            return result;
        }

        LocalDateTime firstStartLocal = BusinessTime.msToBusinessLocalDateTime(BusinessTime.utcLocalDateTimeToMs(firstStart));
        LocalDateTime firstEndLocal = BusinessTime.msToBusinessLocalDateTime(BusinessTime.utcLocalDateTimeToMs(firstEnd));

        LocalDate firstDate = firstStartLocal.toLocalDate();
        if (endDate.isBefore(firstDate) || endDate.isEqual(firstDate)) {
            return result;
        }

        LocalDate cur = nextDate(firstDate, type);
        while (!cur.isAfter(endDate) && result.size() < MAX_INSTANCES) {
            LocalDateTime startLocal = cur.atTime(firstStartLocal.toLocalTime());
            LocalDateTime endLocal = cur.atTime(firstEndLocal.toLocalTime());
            result.add(new long[] {
                BusinessTime.businessLocalDateTimeToMs(startLocal),
                BusinessTime.businessLocalDateTimeToMs(endLocal)
            });
            cur = nextDate(cur, type);
        }
        return result;
    }

    public long[] instanceAt(LocalDateTime firstStart, LocalDateTime firstEnd,
                             RecurrenceType type, int index) {
        if (index <= 0) {
            throw new IllegalArgumentException("index starts from 1");
        }

        LocalDateTime firstStartLocal = BusinessTime.msToBusinessLocalDateTime(BusinessTime.utcLocalDateTimeToMs(firstStart));
        LocalDateTime firstEndLocal = BusinessTime.msToBusinessLocalDateTime(BusinessTime.utcLocalDateTimeToMs(firstEnd));
        LocalDate cur = firstStartLocal.toLocalDate();
        for (int i = 1; i < index; i++) {
            cur = nextDate(cur, type);
        }

        LocalDateTime startLocal = cur.atTime(firstStartLocal.toLocalTime());
        LocalDateTime endLocal = cur.atTime(firstEndLocal.toLocalTime());
        return new long[] {
            BusinessTime.businessLocalDateTimeToMs(startLocal),
            BusinessTime.businessLocalDateTimeToMs(endLocal)
        };
    }

    private LocalDate nextDate(LocalDate from, RecurrenceType type) {
        return switch (type) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case WORKDAY -> {
                LocalDate next = from.plusDays(1);
                while (next.getDayOfWeek().getValue() > 5) {
                    next = next.plusDays(1);
                }
                yield next;
            }
            case MONTHLY -> from.plusMonths(1);
            default -> from.plusDays(1);
        };
    }
}
