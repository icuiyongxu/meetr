package com.meetr.controller;

import com.meetr.application.BookingApplicationService;
import com.meetr.application.dto.BookingApprovalRequest;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.PendingBookingQuery;
import com.meetr.config.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingApplicationService bookingApplicationService;

    @RequirePermission("booking:approve")
    @GetMapping("/pending")
    public ApiResponse<BookingApplicationService.PageResult<BookingDTO>> pending(@ModelAttribute PendingBookingQuery query) {
        if (query.getSize() <= 0) query.setSize(10);
        if (query.getSize() > 100) query.setSize(100);
        if (query.getPage() < 0) query.setPage(0);
        return ApiResponse.ok(bookingApplicationService.getPendingBookings(query));
    }

    @RequirePermission("booking:approve")
    @PutMapping("/{id}/approve")
    public ApiResponse<BookingDTO> approve(@PathVariable Long id, @RequestBody BookingApprovalRequest request) {
        return ApiResponse.ok(bookingApplicationService.approveBooking(id, request.getOperatorId(), request.getRemark()));
    }

    @RequirePermission("booking:approve")
    @PutMapping("/{id}/reject")
    public ApiResponse<BookingDTO> reject(@PathVariable Long id, @RequestBody BookingApprovalRequest request) {
        return ApiResponse.ok(bookingApplicationService.rejectBooking(id, request.getOperatorId(), request.getRemark()));
    }
}
