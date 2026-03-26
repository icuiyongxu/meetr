package com.meetr.domain.entity;

import com.meetr.domain.enums.BuildingStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Building extends BaseEntity {

    private Long id;
    private String name;
    private String campus;
    private String address;
    private Integer sortNo = 0;
    private BuildingStatus status = BuildingStatus.ACTIVE;
}
