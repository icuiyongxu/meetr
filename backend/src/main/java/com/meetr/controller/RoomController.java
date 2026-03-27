package com.meetr.controller;

import com.meetr.application.BookingApplicationService;
import com.meetr.application.RoomApplicationService;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.RoomDTO;
import com.meetr.application.dto.RoomStatusUpdateRequest;
import com.meetr.application.dto.RoomUpsertRequest;
import com.meetr.config.RequirePermission;
import com.meetr.domain.enums.RoomStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class RoomController {

    private final RoomApplicationService roomApplicationService;
    private final BookingApplicationService bookingApplicationService;

    @GetMapping("/api/rooms")
    public ApiResponse<List<RoomDTO>> list(@RequestParam(required = false) Long buildingId,
                                           @RequestParam(required = false) String floor,
                                           @RequestParam(required = false) Integer capacity,
                                           @RequestParam(required = false) RoomStatus status,
                                           @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(roomApplicationService.list(buildingId, floor, capacity, status, keyword));
    }

    /** 日历视图用：dayMillis = UTC 毫秒（北京时间当天 00:00:00） */
    @RequirePermission("booking:view")
    @GetMapping("/api/rooms/schedule")
    public ApiResponse<List<BookingDTO>> getRoomSchedule(
            @RequestParam Long roomId,
            @RequestParam Long dayMillis) {
        Long dayStartMs = dayMillis;
        Long dayEndMs = dayMillis + 86_400_000L;
        return ApiResponse.ok(bookingApplicationService.getBookingsByRoomAndDate(roomId, dayStartMs, dayEndMs));
    }

    @GetMapping("/api/rooms/available")
    public ApiResponse<List<RoomDTO>> available(@RequestParam Long startTimeMs,
                                                @RequestParam Long endTimeMs,
                                                @RequestParam(required = false) Long buildingId,
                                                @RequestParam(required = false) Integer capacity) {
        return ApiResponse.ok(roomApplicationService.available(startTimeMs, endTimeMs, buildingId, capacity));
    }

    @GetMapping("/api/rooms/{id}")
    public ApiResponse<RoomDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomApplicationService.getById(id));
    }

    @RequirePermission("room:manage")
    @PostMapping("/api/admin/rooms")
    public ApiResponse<RoomDTO> create(@Valid @RequestBody RoomUpsertRequest request) {
        return ApiResponse.ok(roomApplicationService.create(request));
    }

    @RequirePermission("room:manage")
    @PutMapping("/api/admin/rooms/{id}")
    public ApiResponse<RoomDTO> update(@PathVariable Long id, @Valid @RequestBody RoomUpsertRequest request) {
        return ApiResponse.ok(roomApplicationService.update(id, request));
    }

    @RequirePermission("room:manage")
    @PutMapping("/api/admin/rooms/{id}/status")
    public ApiResponse<RoomDTO> updateStatus(@PathVariable Long id, @Valid @RequestBody RoomStatusUpdateRequest request) {
        return ApiResponse.ok(roomApplicationService.updateStatus(id, request));
    }
}
