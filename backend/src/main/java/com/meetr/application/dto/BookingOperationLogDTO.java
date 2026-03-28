package com.meetr.application.dto;

import lombok.Data;

@Data
public class BookingOperationLogDTO {
    private Long id;
    private Long bookingId;
    private String operationType;
    private String operatorId;
    private String operatorName;
    private String content;
    private Long createdAtMs;
}
