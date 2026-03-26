package com.meetr.application.dto;

import com.meetr.domain.enums.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 前端传 UTC 毫秒时间戳，后端直接使用，不做任何时区解读。
 * 前端示例: 1742983200000 (北京时间 2026-03-27 00:00:00 的 UTC 毫秒)
 */
@Data
public class CreateBookingCommand {

    @NotNull
    private Long roomId;

    @NotBlank
    private String subject;

    @NotBlank
    private String bookerId;

    private String bookerName;

    /** UTC 毫秒时间戳 */
    @NotNull
    private Long startTime;

    /** UTC 毫秒时间戳 */
    @NotNull
    private Long endTime;

    private Integer attendeeCount;

    private List<String> attendeeIds;

    private String remark;

    /** 重复类型，默认不重复 */
    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    /** 重复结束日期（不含）；recurrenceType 为 NONE 时忽略 */
    private LocalDate recurrenceEndDate;
}
