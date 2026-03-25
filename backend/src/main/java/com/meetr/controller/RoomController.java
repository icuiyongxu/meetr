package com.meetr.controller;

import com.meetr.application.RoomApplicationService;
import com.meetr.application.dto.RoomDTO;
import com.meetr.application.dto.RoomStatusUpdateRequest;
import com.meetr.application.dto.RoomUpsertRequest;
import com.meetr.domain.enums.RoomStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class RoomController {

    private final RoomApplicationService roomApplicationService;

    @GetMapping("/api/rooms")
    public ApiResponse<List<RoomDTO>> list(@RequestParam(required = false) Long buildingId,
                                           @RequestParam(required = false) String floor,
                                           @RequestParam(required = false) Integer capacity,
                                           @RequestParam(required = false) RoomStatus status,
                                           @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(roomApplicationService.list(buildingId, floor, capacity, status, keyword));
    }

    @GetMapping("/api/rooms/available")
    public ApiResponse<List<RoomDTO>> available(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                @RequestParam(required = false) Long buildingId,
                                                @RequestParam(required = false) Integer capacity) {
        return ApiResponse.ok(roomApplicationService.available(startTime, endTime, buildingId, capacity));
    }

    @GetMapping("/api/rooms/{id}")
    public ApiResponse<RoomDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomApplicationService.getById(id));
    }

    @PostMapping("/api/admin/rooms")
    public ApiResponse<RoomDTO> create(@Valid @RequestBody RoomUpsertRequest request) {
        return ApiResponse.ok(roomApplicationService.create(request));
    }

    @PutMapping("/api/admin/rooms/{id}")
    public ApiResponse<RoomDTO> update(@PathVariable Long id, @Valid @RequestBody RoomUpsertRequest request) {
        return ApiResponse.ok(roomApplicationService.update(id, request));
    }

    @PutMapping("/api/admin/rooms/{id}/status")
    public ApiResponse<RoomDTO> updateStatus(@PathVariable Long id, @Valid @RequestBody RoomStatusUpdateRequest request) {
        return ApiResponse.ok(roomApplicationService.updateStatus(id, request));
    }
}
