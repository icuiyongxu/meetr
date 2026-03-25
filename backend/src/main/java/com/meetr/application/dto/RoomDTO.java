package com.meetr.application.dto;

import com.meetr.domain.enums.RoomStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoomDTO {

    private Long id;
    private Long buildingId;
    private String buildingName;
    private String name;
    private String floor;
    private Integer capacity;
    private String equipment;
    private List<String> equipmentItems = new ArrayList<>();
    private RoomStatus status;
    private String remark;
}
