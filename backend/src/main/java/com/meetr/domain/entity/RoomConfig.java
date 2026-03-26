package com.meetr.domain.entity;

import com.meetr.domain.enums.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@Entity
@Table(name = "room_config")
@EqualsAndHashCode(callSuper = true)
public class RoomConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;

    @Column(nullable = false)
    private Integer resolution = 1800;

    @Column(nullable = false)
    private Integer defaultDuration = 60;

    @Column(nullable = false, length = 5)
    private String morningStarts = "08:00";

    @Column(nullable = false, length = 5)
    private String eveningEnds = "22:00";

    @Column(nullable = false)
    private Integer minBookAheadMinutes = 0;

    @Column(nullable = false)
    private Integer maxBookAheadDays = 30;

    @Column(nullable = false)
    private Integer minDurationMinutes = 15;

    @Column(nullable = false)
    private Integer maxDurationMinutes = 480;

    @Column(nullable = false)
    private Integer maxPerDay = 20;

    @Column(nullable = false)
    private Integer maxPerWeek = 10;

    @Column(nullable = false)
    private Boolean approvalRequired = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
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
