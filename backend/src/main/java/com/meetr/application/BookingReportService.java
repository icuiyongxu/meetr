package com.meetr.application;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.mapper.BookingMapper;
import com.meetr.mapper.BuildingMapper;
import com.meetr.mapper.MeetingRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingReportService {

    private final BookingMapper bookingMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final BuildingMapper buildingMapper;

    /** 每天可用时长（小时），后续可从规则配置读取 */
    private static final int DAILY_AVAILABLE_HOURS = 14;

    /**
     * 会议室使用率统计。
     */
    public List<RoomUsageVO> roomUsage(LocalDate startDate, LocalDate endDate, Long buildingId) {
        long startMs = toMs(startDate);
        long endMs = toMs(endDate.plusDays(1));

        // 1. 找出所有会议室（含楼栋名）
        List<MeetingRoom> rooms = buildingId != null
            ? meetingRoomMapper.findByBuildingId(buildingId)
            : meetingRoomMapper.findAll();
        Map<Long, MeetingRoom> roomMap = rooms.stream()
            .collect(Collectors.toMap(MeetingRoom::getId, r -> r));
        if (roomMap.isEmpty()) return List.of();

        Map<Long, String> buildingNameMap = buildingMapper.findAllByOrderBySortNoAscIdAsc().stream()
            .collect(Collectors.toMap(com.meetr.domain.entity.Building::getId, com.meetr.domain.entity.Building::getName));

        // 2. 拉有效预约（已确认的）
        List<Booking> validBookings = bookingMapper.findValidBookingsInRange(startMs, endMs);
        Map<Long, List<Booking>> byRoom = validBookings.stream()
            .collect(Collectors.groupingBy(Booking::getRoomId));

        // 3. 拉取消预约
        List<Booking> canceledBookings = bookingMapper.findCanceledBookingsInRange(startMs, endMs);
        Map<Long, Long> cancelCountByRoom = canceledBookings.stream()
            .collect(Collectors.groupingBy(Booking::getRoomId, Collectors.counting()));

        // 4. 拉待审批（期末时点）
        List<Booking> pendingBookings = bookingMapper.findPendingBookingsInRange(startMs, endMs);
        Map<Long, Long> pendingCountByRoom = pendingBookings.stream()
            .collect(Collectors.groupingBy(Booking::getRoomId, Collectors.counting()));

        // 5. 计算可用总时长（天数 * 14h）
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int availableMinutesPerRoom = (int) (totalDays * DAILY_AVAILABLE_HOURS * 60);

        List<RoomUsageVO> result = new ArrayList<>();
        for (MeetingRoom room : rooms) {
            List<Booking> rBookings = byRoom.getOrDefault(room.getId(), List.of());
            long totalMinutes = rBookings.stream()
                .mapToLong(b -> b.getEndTimeMs() - b.getStartTimeMs())
                .sum() / 60_000;
            double usagePercent = availableMinutesPerRoom > 0
                ? Math.min(100.0, totalMinutes * 100.0 / availableMinutesPerRoom)
                : 0.0;

            result.add(new RoomUsageVO(
                room.getId(),
                room.getName(),
                room.getBuildingId(),
                buildingNameMap.getOrDefault(room.getBuildingId(), ""),
                rBookings.size(),
                totalMinutes,
                Math.round(usagePercent * 10) / 10.0,
                cancelCountByRoom.getOrDefault(room.getId(), 0L),
                pendingCountByRoom.getOrDefault(room.getId(), 0L)
            ));
        }

        // 按使用率降序
        result.sort((a, b) -> Double.compare(b.usagePercent(), a.usagePercent()));
        return result;
    }

    /**
     * 预约记录查询（分页）。
     */
    public Map<String, Object> bookingRecords(
            String buildingIds,
            String roomIds,
            String bookerId,
            String keyword,
            String status,
            String approvalStatus,
            Long startFromMs,
            Long startToMs,
            int page,
            int size) {
        try {
        log.info("bookingRecords params: buildingIds={}, roomIds={}, bookerId={}, keyword={}, status={}, approvalStatus={}, startFromMs={}, startToMs={}, page={}, size={}",
            buildingIds, roomIds, bookerId, keyword, status, approvalStatus, startFromMs, startToMs , page, size);

        // 默认查近30天
        long fromMs = startFromMs != null ? startFromMs
            : LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long toMs = startToMs != null ? startToMs
            : System.currentTimeMillis();

        List<Long> buildingIdList = parseIdList(buildingIds);
        List<Long> roomIdList = parseIdList(roomIds);

        if (size == -1) size = 10_000; // 导出模式

        log.info("findBookingsForReport => buildingIdList={}, roomIdList={}, bookerId={}, keyword={}, fromMs={}, toMs={}",
            buildingIdList, roomIdList, bookerId, keyword, fromMs, toMs);
        PageHelper.startPage(page + 1, size);
        List<Booking> bookings = bookingMapper.findBookingsForReport(
            bookerId, keyword, status, approvalStatus, fromMs, toMs);
        PageInfo<Booking> pageInfo = new PageInfo<>(bookings);
        log.info("findBookingsForReport returned {} bookings, total={}", bookings.size(), pageInfo.getTotal());

        // 填充楼栋/会议室名称
        Map<Long, MeetingRoom> roomMap = meetingRoomMapper.findAll().stream()
            .collect(Collectors.toMap(MeetingRoom::getId, r -> r));
        Map<Long, String> buildingNameMap = buildingMapper.findAllByOrderBySortNoAscIdAsc().stream()
            .collect(Collectors.toMap(com.meetr.domain.entity.Building::getId, com.meetr.domain.entity.Building::getName));

        List<Map<String, Object>> records = bookings.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("subject", b.getSubject());
            MeetingRoom r = roomMap.get(b.getRoomId());
            m.put("roomId", b.getRoomId());
            m.put("roomName", r != null ? r.getName() : null);
            m.put("buildingId", r != null ? r.getBuildingId() : null);
            m.put("buildingName", r != null ? buildingNameMap.getOrDefault(r.getBuildingId(), "") : "");
            m.put("bookerId", b.getBookerId());
            m.put("bookerName", b.getBookerName());
            m.put("startTime", b.getStartTimeMs());
            m.put("endTime", b.getEndTimeMs());
            m.put("durationMinutes", (b.getEndTimeMs() - b.getStartTimeMs()) / 60_000);
            m.put("status", b.getStatus());
            m.put("approvalStatus", b.getApprovalStatus());
            m.put("createdAtMs", b.getCreatedAtMs());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", records);
        result.put("totalElements", pageInfo.getTotal());
        return result;
        } catch (Exception e) {
            log.error("bookingRecords error", e);
            throw e;
        }
    }

    /**
     * 预约记录导出（不分页，最多10000条）。
     */
    public List<Map<String, Object>> exportBookingRecords(
            String bookerId,
            String keyword,
            String status,
            String approvalStatus,
            Long startFromMs,
            Long startToMs) {
        long fromMs = startFromMs != null ? startFromMs
            : LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long toMs = startToMs != null ? startToMs
            : System.currentTimeMillis();

        // 不走分页，直接查 mapper（有 LIMIT 10000）
        List<Booking> bookings = bookingMapper.findBookingsForReport(
            bookerId, keyword, status, approvalStatus, fromMs, toMs);

        Map<Long, MeetingRoom> roomMap = meetingRoomMapper.findAll().stream()
            .collect(java.util.stream.Collectors.toMap(MeetingRoom::getId, r -> r));
        Map<Long, String> buildingNameMap = buildingMapper.findAllByOrderBySortNoAscIdAsc().stream()
            .collect(java.util.stream.Collectors.toMap(
                com.meetr.domain.entity.Building::getId,
                com.meetr.domain.entity.Building::getName));

        return bookings.stream().map(b -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("主题", b.getSubject());
            MeetingRoom r = roomMap.get(b.getRoomId());
            m.put("楼栋", r != null ? buildingNameMap.getOrDefault(r.getBuildingId(), "") : "");
            m.put("会议室", r != null ? r.getName() : "");
            m.put("预约人", b.getBookerName());
            m.put("开始时间", formatTime(b.getStartTimeMs()));
            m.put("结束时间", formatTime(b.getEndTimeMs()));
            m.put("时长(分钟)", (b.getEndTimeMs() - b.getStartTimeMs()) / 60_000);
            m.put("预约状态", "BOOKED".equals(b.getStatus().name()) ? "已确认" : "已取消");
            m.put("审批状态", mapApprovalStatus(b.getApprovalStatus().name()));
            return m;
        }).toList();
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "";
        return java.time.Instant.ofEpochMilli(ms)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String mapApprovalStatus(String as) {
        return switch (as) {
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            case "PENDING" -> "待审批";
            case "NONE" -> "无需审批";
            default -> as;
        };
    }

    /**
     * 用户维度统计。
     */
    public List<UserUsageVO> userUsage(LocalDate startDate, LocalDate endDate) {
        long startMs = toMs(startDate);
        long endMs = toMs(endDate.plusDays(1));

        List<Booking> bookings = bookingMapper.findBookingsInRange(startMs, endMs);

        // 按 bookerId 分组
        Map<String, List<Booking>> byBooker = bookings.stream()
            .collect(Collectors.groupingBy(b ->
                b.getBookerId() != null ? b.getBookerId() : "__unknown__"));

        return byBooker.entrySet().stream().map(e -> {
            String bookerId = e.getKey();
            List<Booking> list = e.getValue();
            long validCount = list.stream()
                .filter(b -> "BOOKED".equals(b.getStatus().name())
                    && ("APPROVED".equals(b.getApprovalStatus().name()) || "NONE".equals(b.getApprovalStatus().name())))
                .count();
            long canceledCount = list.stream()
                .filter(b -> "CANCELED".equals(b.getStatus().name()))
                .count();
            long rejectedCount = list.stream()
                .filter(b -> "REJECTED".equals(b.getApprovalStatus().name()))
                .count();
            long totalMinutes = list.stream()
                .filter(b -> "BOOKED".equals(b.getStatus().name()))
                .mapToLong(b -> (b.getEndTimeMs() - b.getStartTimeMs()) / 60_000)
                .sum();
            String bookerName = list.stream()
                .findFirst().map(Booking::getBookerName).orElse(bookerId);

            return new UserUsageVO(
                bookerId,
                bookerName,
                list.size(),
                validCount,
                canceledCount,
                rejectedCount,
                totalMinutes
            );
        }).sorted((a, b) -> Long.compare(b.totalBookings(), a.totalBookings()))
          .toList();
    }

    // ── helpers ────────────────────────────────────────────────

    private long toMs(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private List<Long> parseIdList(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
            .map(String::trim)
            .filter(t -> !t.isEmpty())
            .map(Long::parseLong)
            .toList();
    }

    // ── VO ─────────────────────────────────────────────────────

    public record RoomUsageVO(
        Long roomId,
        String roomName,
        Long buildingId,
        String buildingName,
        long totalBookings,
        long totalMinutes,
        double usagePercent,
        long canceledCount,
        long pendingCount
    ) {}

    public record UserUsageVO(
        String bookerId,
        String bookerName,
        long totalBookings,
        long validBookings,
        long canceledCount,
        long rejectedCount,
        long totalMinutes
    ) {}
}
