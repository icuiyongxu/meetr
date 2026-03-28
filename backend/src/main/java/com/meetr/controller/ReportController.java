package com.meetr.controller;

import com.meetr.application.BookingReportService;
import com.meetr.config.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final BookingReportService reportService;

    @RequirePermission("booking:view")
    @GetMapping("/room-usage")
    public ApiResponse<?> roomUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long buildingId) {
        return ApiResponse.ok(reportService.roomUsage(startDate, endDate, buildingId));
    }

    @RequirePermission("booking:view")
    @GetMapping("/booking-records")
    public ApiResponse<?> bookingRecords(
            @RequestParam(required = false) String buildingIds,
            @RequestParam(required = false) String roomIds,
            @RequestParam(required = false) String bookerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Long startFromMs,
            @RequestParam(required = false) Long startToMs,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reportService.bookingRecords(
            buildingIds, roomIds, bookerId, keyword, status, approvalStatus,
            startFromMs, startToMs, page, size));
    }

    @RequirePermission("booking:view")
    @GetMapping("/user-usage")
    public ApiResponse<?> userUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(reportService.userUsage(startDate, endDate));
    }
}
