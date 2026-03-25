package com.meetr.controller;

import com.meetr.application.RoomConfigApplicationService;
import com.meetr.application.dto.RoomConfigDTO;
import com.meetr.application.dto.SaveRoomConfigRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/booking-rules")
public class ConfigController {

    private final RoomConfigApplicationService roomConfigApplicationService;

    @GetMapping
    public ApiResponse<RoomConfigDTO> get(@RequestParam(required = false) Long roomId) {
        return ApiResponse.ok(roomConfigApplicationService.getEffectiveConfig(roomId));
    }

    @PostMapping
    public ApiResponse<RoomConfigDTO> save(@Valid @RequestBody SaveRoomConfigRequest request) {
        return ApiResponse.ok(roomConfigApplicationService.save(request));
    }
}
