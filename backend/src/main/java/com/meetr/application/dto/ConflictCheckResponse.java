package com.meetr.application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConflictCheckResponse {

    private boolean conflict;
    private LocalDateTime alignedStartTime;
    private LocalDateTime alignedEndTime;
    private List<BookingConflictDTO> conflictingBookings = new ArrayList<>();
}
