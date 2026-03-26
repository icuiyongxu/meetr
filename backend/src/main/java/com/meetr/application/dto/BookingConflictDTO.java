package com.meetr.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingConflictDTO {

    private Long id;
    private String subject;
    /** UTC 毫秒时间戳 */
    private Long startTime;
    /** UTC 毫秒时间戳 */
    private Long endTime;
    private String bookerName;
}
