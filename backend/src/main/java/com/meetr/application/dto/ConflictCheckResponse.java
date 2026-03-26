package com.meetr.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConflictCheckResponse {

    private boolean conflict;

    /** 对齐后的 UTC 毫秒时间戳 */
    private Long alignedStartTime;

    private Long alignedEndTime;

    private List<BookingConflictDTO> conflictingBookings;
}
