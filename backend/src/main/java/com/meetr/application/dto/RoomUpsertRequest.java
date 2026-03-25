package com.meetr.application.dto;

import com.meetr.domain.enums.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoomUpsertRequest {

    @NotNull
    private Long buildingId;

    @NotBlank
    private String name;

    private String floor;

    @Min(0)
    private Integer capacity = 0;

    private List<String> equipmentItems = new ArrayList<>();

    private RoomStatus status = RoomStatus.ENABLED;

    private String remark;
}
