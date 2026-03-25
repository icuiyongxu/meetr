package com.meetr.application.dto;

import com.meetr.domain.enums.BuildingStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BuildingUpsertRequest {

    @NotBlank
    private String name;

    private String campus;

    private String address;

    private Integer sortNo = 0;

    private BuildingStatus status = BuildingStatus.ACTIVE;
}
