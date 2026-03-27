package com.meetr.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class SeriesBookingResponse {
    /** 系列主预约（seriesIndex = 1） */
    private BookingDTO master;
    /** 系列所有子预约（不含 master），按 seriesIndex 升序 */
    private List<BookingDTO> instances;
    /** 该系列总场次 */
    private int totalCount;
}
