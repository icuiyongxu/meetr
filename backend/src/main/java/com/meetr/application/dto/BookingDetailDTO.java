package com.meetr.application.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookingDetailDTO {
    private BookingDTO booking;
    private List<BookingOperationLogDTO> operationLogs = new ArrayList<>();
}
