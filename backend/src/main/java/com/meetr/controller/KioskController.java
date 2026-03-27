package com.meetr.controller;

import com.meetr.controller.ApiResponse;
import com.meetr.application.dto.KioskBookingDto;
import com.meetr.application.dto.KioskResponse;
import com.meetr.application.dto.KioskRoomDto;
import com.meetr.domain.entity.Booking;
import com.meetr.domain.entity.Building;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.mapper.BuildingMapper;
import com.meetr.mapper.KioskMapper;
import com.meetr.mapper.MeetingRoomMapper;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 大屏/看板模式接口，无需登录即可访问。
 * GET /api/kiosk/rooms/{roomId}?dateMs=xxx
 */
@RestController
@RequestMapping("/api/kiosk")
public class KioskController {

    private final KioskMapper kioskMapper;
    private final MeetingRoomMapper roomMapper;
    private final BuildingMapper buildingMapper;

    public KioskController(KioskMapper kioskMapper,
                          MeetingRoomMapper roomMapper,
                          BuildingMapper buildingMapper) {
        this.kioskMapper = kioskMapper;
        this.roomMapper = roomMapper;
        this.buildingMapper = buildingMapper;
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<KioskResponse> getRoomBoard(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long dateMs) {

        long targetMs = dateMs != null ? dateMs : System.currentTimeMillis();
        long dayStartMs = dayStartMs(targetMs);
        long dayEndMs = dayStartMs + 86_400_000L;

        MeetingRoom room = roomMapper.findById(roomId);
        if (room == null) {
            return ApiResponse.error(40401, "会议室不存在",null);
        }

        Building building = null;
        if (room.getBuildingId() != null) {
            building = buildingMapper.findById(room.getBuildingId());
        }

        List<Booking> bookings = kioskMapper.findByRoomAndDay(roomId, dayStartMs, dayEndMs);
        long now = System.currentTimeMillis();

        List<KioskBookingDto> dtos = bookings.stream().map(b -> {
            String status;
            if (b.getEndTimeMs() <= now) {
                status = "ENDED";
            } else if (b.getStartTimeMs() <= now) {
                status = "IN_PROGRESS";
            } else {
                status = "UPCOMING";
            }
            return new KioskBookingDto(
                b.getId(),
                b.getSubject(),
                b.getBookerName(),
                b.getStartTimeMs(),
                b.getEndTimeMs(),
                b.getAttendeeCount(),
                b.getRemark(),
                status
            );
        }).toList();

        KioskRoomDto roomDto = new KioskRoomDto(
            room.getId(),
            room.getName(),
            building != null ? building.getName() : ""
        );

        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date(targetMs));
        return ApiResponse.ok(new KioskResponse(roomDto, dateStr, dtos));
    }

    private long dayStartMs(long ms) {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate localDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate();
        return localDate.atStartOfDay(zone).toInstant().toEpochMilli();
    }
}
