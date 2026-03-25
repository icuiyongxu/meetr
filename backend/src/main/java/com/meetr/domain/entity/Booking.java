package com.meetr.domain.entity;

import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.vo.TimeSlot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@Entity
@Table(name = "booking")
@EqualsAndHashCode(callSuper = true)
public class Booking extends BaseEntity {

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 64)
    private String bookerId;

    @Column(length = 100)
    private String bookerName;

    /** UTC 毫秒时间戳，存为 BIGINT */
    @Column(nullable = false)
    private Long startTimeMs;

    /** UTC 毫秒时间戳，存为 BIGINT */
    @Column(nullable = false)
    private Long endTimeMs;

    @Column(nullable = false)
    private Integer attendeeCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.NONE;

    @Column(length = 500)
    private String remark;

    @Version
    @Column(nullable = false)
    private Long version;

    // ── 转换方法 ──────────────────────────────────────

    public LocalDateTime getStartTime() {
        return LocalDateTime.ofEpochSecond(startTimeMs / 1000, 0, UTC);
    }

    public LocalDateTime getEndTime() {
        return LocalDateTime.ofEpochSecond(endTimeMs / 1000, 0, UTC);
    }

    public void setStartTime(LocalDateTime dt) {
        this.startTimeMs = dt.toEpochSecond(UTC) * 1000;
    }

    public void setEndTime(LocalDateTime dt) {
        this.endTimeMs = dt.toEpochSecond(UTC) * 1000;
    }

    public TimeSlot timeSlot() {
        return new TimeSlot(getStartTime(), getEndTime());
    }

    public void applyTimeSlot(TimeSlot timeSlot) {
        setStartTime(timeSlot.start());
        setEndTime(timeSlot.end());
    }

    public void cancel() {
        if (status == BookingStatus.CANCELED) {
            throw new IllegalStateException("Booking already canceled");
        }
        status = BookingStatus.CANCELED;
    }

    public void updateDetails(String subject, TimeSlot timeSlot, Integer attendeeCount, String remark) {
        this.subject = subject;
        applyTimeSlot(timeSlot);
        this.attendeeCount = attendeeCount;
        this.remark = remark;
    }
}
