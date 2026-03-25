package com.meetr.application.dto;

import com.meetr.domain.enums.RoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SaveRoomConfigRequest {

    private Long roomId;

    @Min(60)
    private Integer resolution = 1800;

    @Min(1)
    private Integer defaultDuration = 60;

    private String morningStarts = "08:00";

    private String eveningEnds = "22:00";

    @Min(0)
    private Integer minBookAheadMinutes = 0;

    @Min(0)
    private Integer maxBookAheadDays = 30;

    @Min(1)
    private Integer minDurationMinutes = 15;

    @Min(1)
    @Max(1440)
    private Integer maxDurationMinutes = 480;

    @Min(1)
    private Integer maxPerDay = 3;

    @Min(1)
    private Integer maxPerWeek = 10;

    private Boolean approvalRequired = Boolean.FALSE;

    private RoomStatus status = RoomStatus.ENABLED;
}
