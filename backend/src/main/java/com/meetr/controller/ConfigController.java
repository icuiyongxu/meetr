package com.meetr.controller;

import com.meetr.application.RoomConfigApplicationService;
import com.meetr.application.dto.RoomConfigDTO;
import com.meetr.application.dto.SaveRoomConfigRequest;
import com.meetr.config.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/booking-rules")
public class ConfigController {

    private final RoomConfigApplicationService roomConfigApplicationService;

    @RequirePermission("config:view")
    @GetMapping
    public ApiResponse<RoomConfigDTO> get(@RequestParam(required = false) Long roomId) {
        return ApiResponse.ok(roomConfigApplicationService.getEffectiveConfig(roomId));
    }

    @RequirePermission("config:manage")
    @PostMapping
    public ApiResponse<RoomConfigDTO> save(@Valid @RequestBody SaveRoomConfigRequest request) {
        return ApiResponse.ok(roomConfigApplicationService.save(request));
    }
}
