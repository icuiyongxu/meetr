package com.meetr.application.dto;

import com.meetr.domain.enums.BuildingStatus;
import lombok.Data;

@Data
public class BuildingDTO {

    private Long id;
    private String name;
    private String campus;
    private String address;
    private Integer sortNo;
    private BuildingStatus status;
}
