package com.meetr.domain.entity;

import com.meetr.domain.enums.RoomStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoomConfig extends BaseEntity {

    private Long id;
    private Long roomId;
    private Integer resolution = 1800;
    private Integer defaultDuration = 60;
    private String morningStarts = "08:00";
    private String eveningEnds = "22:00";
    private Integer minBookAheadMinutes = 0;
    private Integer maxBookAheadDays = 30;
    private Integer minDurationMinutes = 15;
    private Integer maxDurationMinutes = 480;
    private Integer maxPerDay = 20;
    private Integer maxPerWeek = 10;
    private Boolean approvalRequired = Boolean.FALSE;
    private RoomStatus status = RoomStatus.ENABLED;
    /** 管理员 userId 列表，逗号分隔。匹配到即为 ADMIN 角色 */
    private String adminUserIds;

    public LocalTime getMorningStartsAsTime() {
        return LocalTime.parse(morningStarts);
    }

    public LocalTime getEveningEndsAsTime() {
        return LocalTime.parse(eveningEnds);
    }
}
