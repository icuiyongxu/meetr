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

@Data
@Entity
@Table(name = "booking")
@EqualsAndHashCode(callSuper = true)
public class Booking extends BaseEntity {

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

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

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

    public TimeSlot timeSlot() {
        return new TimeSlot(startTime, endTime);
    }

    public void applyTimeSlot(TimeSlot timeSlot) {
        this.startTime = timeSlot.start();
        this.endTime = timeSlot.end();
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
