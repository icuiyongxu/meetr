package com.meetr.domain.service;

import com.meetr.domain.enums.RecurrenceType;
import com.meetr.domain.vo.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    /**
     * 展开所有子实例（不含第一次），返回每次的 startMs/endMs。
     * 如果 recurrenceType == NONE，返回空列表。
     */
    public List<long[]> expand(LocalDateTime firstStart, LocalDateTime firstEnd,
                                RecurrenceType type, LocalDate endDate) {
        List<long[]> result = new ArrayList<>();
        if (type == null || type == RecurrenceType.NONE || endDate == null) {
            return result;
        }

        LocalDate firstDate = firstStart.toLocalDate();
        if (endDate.isBefore(firstDate) || endDate.isEqual(firstDate)) {
            return result;
        }

        long durationMs = firstEnd.toEpochSecond(UTC) * 1000 - firstStart.toEpochSecond(UTC) * 1000;
        LocalDate cur = nextDate(firstDate, type);

        while (!cur.isAfter(endDate) && result.size() < MAX_INSTANCES) {
            LocalDateTime s = cur.atTime(firstStart.toLocalTime());
            LocalDateTime e = cur.atTime(firstEnd.toLocalTime());
            result.add(new long[] {
                s.toEpochSecond(UTC) * 1000,
                e.toEpochSecond(UTC) * 1000
            });
            cur = nextDate(cur, type);
        }
        return result;
    }

    /**
     * 计算从 start 起第 n 个实例（n 从1开始）的 UTC 毫秒。
     * 用于按需生成（目前暂未启用，先做一次性展开）。
     */
    public long[] instanceAt(LocalDateTime firstStart, LocalDateTime firstEnd,
                              RecurrenceType type, int index) {
        if (index <= 0) {
            throw new IllegalArgumentException("index starts from 1");
        }
        long durationMs = firstEnd.toEpochSecond(UTC) * 1000 - firstStart.toEpochSecond(UTC) * 1000;
        LocalDate firstDate = firstStart.toLocalDate();
        LocalDate cur = firstDate;
        for (int i = 1; i < index; i++) {
            cur = nextDate(cur, type);
        }
        LocalDateTime s = cur.atTime(firstStart.toLocalTime());
        return new long[] {
            s.toEpochSecond(UTC) * 1000,
            s.toEpochSecond(UTC) * 1000 + durationMs
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
            case MONTHLY -> {
                LocalDate next = from.plusMonths(1);
                // 如果日号溢出（如 31→2月），取该月最后一天
                while (next.getDayOfMonth() != from.getDayOfMonth() && next.getMonth() != from.getMonth().plus(1)) {
                    next = next.minusDays(1);
                }
                yield next;
            }
            default -> from.plusDays(1);
        };
    }
}
