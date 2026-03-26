package com.meetr.domain.entity;

import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.BookingStatus;
import com.meetr.domain.enums.RecurrenceType;
import com.meetr.domain.vo.TimeSlot;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
@EqualsAndHashCode(callSuper = true)
public class Booking extends BaseEntity {

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private Long id;
    private Long roomId;
    private String subject;
    private String bookerId;
    private String bookerName;
    /** UTC 毫秒时间戳，存为 BIGINT */
    private Long startTimeMs;
    /** UTC 毫秒时间戳，存为 BIGINT */
    private Long endTimeMs;
    private Integer attendeeCount = 1;
    private BookingStatus status = BookingStatus.BOOKED;
    private ApprovalStatus approvalStatus = ApprovalStatus.NONE;
    private String remark;
    private Long version = 0L;
    /** 重复类型，NONE 表示不重复 */
    private RecurrenceType recurrenceType = RecurrenceType.NONE;
    /** 重复结束日期（不含），超过该日期不再生成实例 */
    private LocalDate recurrenceEndDate;
    /** 指向主预约；主预约自身为 null */
    private Long parentId;
    /** 该实例在系列中的序号（从1起），主预约=1 */
    private Integer seriesIndex = 0;

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
