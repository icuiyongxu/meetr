package com.meetr.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBookingCommand {

    @NotNull
    private Long roomId;

    @NotBlank
    private String subject;

    @NotBlank
    private String bookerId;

    private String bookerName;

    /** 格式: 2026-03-26T02:00:00，后端统一按北京时间解读 */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;

    /** 格式: 2026-03-26T02:00:00，后端统一按北京时间解读 */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime endTime;

    private Integer attendeeCount;

    private List<String> attendeeIds;

    private String remark;
}
