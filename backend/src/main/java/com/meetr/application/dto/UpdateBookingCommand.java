package com.meetr.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateBookingCommand {

    @NotNull
    private Long bookingId;

    @NotBlank
    private String subject;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;

    @Min(1)
    private Integer attendeeCount = 1;

    private String remark;

    @NotBlank
    private String operatorId;
}
