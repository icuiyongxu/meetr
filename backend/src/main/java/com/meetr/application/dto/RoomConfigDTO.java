package com.meetr.application.dto;

import com.meetr.domain.enums.RoomStatus;
import lombok.Data;

@Data
public class RoomConfigDTO {

    private Long id;
    private Long roomId;
    private Integer resolution;
    private Integer defaultDuration;
    private String morningStarts;
    private String eveningEnds;
    private Integer minBookAheadMinutes;
    private Integer maxBookAheadDays;
    private Integer minDurationMinutes;
    private Integer maxDurationMinutes;
    private Integer maxPerDay;
    private Integer maxPerWeek;
    private Boolean approvalRequired;
    private RoomStatus status;
}
