package com.meetr.controller;

import com.meetr.application.BuildingApplicationService;
import com.meetr.application.dto.BuildingDTO;
import com.meetr.application.dto.BuildingUpsertRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class BuildingController {

    private final BuildingApplicationService buildingApplicationService;

    @GetMapping("/api/buildings")
    public ApiResponse<List<BuildingDTO>> list() {
        return ApiResponse.ok(buildingApplicationService.list());
    }

    @PostMapping("/api/admin/buildings")
    public ApiResponse<BuildingDTO> create(@Valid @RequestBody BuildingUpsertRequest request) {
        return ApiResponse.ok(buildingApplicationService.create(request));
    }

    @PutMapping("/api/admin/buildings/{id}")
    public ApiResponse<BuildingDTO> update(@PathVariable Long id, @Valid @RequestBody BuildingUpsertRequest request) {
        return ApiResponse.ok(buildingApplicationService.update(id, request));
    }
}
