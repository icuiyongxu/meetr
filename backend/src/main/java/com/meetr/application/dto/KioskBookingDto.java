package com.meetr.application.dto;

public record KioskBookingDto(
    Long id,
    String subject,
    String bookerName,
    Long startTimeMs,
    Long endTimeMs,
    Integer attendeeCount,
    String remark,
    String status
) {}
