package com.meetr.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 前端传 UTC 毫秒时间戳，后端直接使用。
 */
@Data
public class ConflictCheckRequest {

    @NotNull
    private Long roomId;

    /** UTC 毫秒时间戳 */
    @NotNull
    private Long startTime;

    /** UTC 毫秒时间戳 */
    @NotNull
    private Long endTime;

    private Long excludeBookingId;
}
