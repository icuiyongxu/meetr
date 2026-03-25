package com.meetr.application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConflictCheckResponse {

    private boolean conflict;

    /** 北京时间字符串，前端用 dayjs.tz() 解析 */
    private LocalDateTime alignedStartTime;

    private LocalDateTime alignedEndTime;

    private List<BookingConflictDTO> conflictingBookings;
}
