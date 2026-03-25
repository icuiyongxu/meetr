package com.meetr.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingConflictDTO {

    private Long id;
    private String subject;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String bookerName;
}
