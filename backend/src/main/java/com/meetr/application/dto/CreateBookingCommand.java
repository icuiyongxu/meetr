package com.meetr.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;

    @Min(1)
    private Integer attendeeCount = 1;

    private String remark;

    private List<String> attendeeIds = new ArrayList<>();
}
