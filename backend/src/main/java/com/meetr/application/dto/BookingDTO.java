package com.meetr.application.dto;

import com.meetr.domain.enums.ApprovalStatus;
import com.meetr.domain.enums.BookingStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookingDTO {

    private Long id;
    private Long roomId;
    private String roomName;
    private Long buildingId;
    private String buildingName;
    private String subject;
    private String bookerId;
    private String bookerName;
    /** UTC 毫秒时间戳 */
    private Long startTime;
    /** UTC 毫秒时间戳 */
    private Long endTime;
    private Integer attendeeCount;
    private BookingStatus status;
    private ApprovalStatus approvalStatus;
    private String remark;
    private Long version;
    private List<BookingAttendeeDTO> attendees = new ArrayList<>();
}
