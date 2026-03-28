package com.meetr.controller;

import com.meetr.application.BuildingApplicationService;
import com.meetr.application.dto.BuildingDTO;
import com.meetr.application.dto.BuildingUpsertRequest;
import com.meetr.config.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class BuildingController {

    private final BuildingApplicationService buildingApplicationService;

    @RequirePermission("building:view")
    @GetMapping("/api/buildings")
    public ApiResponse<List<BuildingDTO>> list() {
        return ApiResponse.ok(buildingApplicationService.list());
    }

    @RequirePermission("building:manage")
    @PostMapping("/api/admin/buildings")
    public ApiResponse<BuildingDTO> create(@Valid @RequestBody BuildingUpsertRequest request) {
        return ApiResponse.ok(buildingApplicationService.create(request));
    }

    @RequirePermission("building:manage")
    @PutMapping("/api/admin/buildings/{id}")
    public ApiResponse<BuildingDTO> update(@PathVariable Long id, @Valid @RequestBody BuildingUpsertRequest request) {
        return ApiResponse.ok(buildingApplicationService.update(id, request));
    }
}
