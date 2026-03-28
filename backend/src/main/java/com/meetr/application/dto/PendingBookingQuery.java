package com.meetr.application.dto;

import lombok.Data;

@Data
public class PendingBookingQuery {

    private Long buildingId;
    private Long roomId;
    private String bookerId;
    private String keyword;
    private Long startDateMs;
    private Long endDateMs;
    private int page = 0;
    private int size = 10;
}
