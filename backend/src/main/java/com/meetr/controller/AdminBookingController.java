package com.meetr.controller;

import com.meetr.application.BookingApplicationService;
import com.meetr.application.dto.BookingApprovalRequest;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.PendingBookingQuery;
import com.meetr.config.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PutMapping("/batch-approve")
    public ApiResponse<BatchResult> batchApprove(@RequestBody @Valid BatchRequest request) {
        int success = 0;
        int skipped = 0;
        for (Long id : request.getBookingIds()) {
            try {
                bookingApplicationService.approveBooking(id, request.getOperatorId(), request.getRemark());
                success++;
            } catch (Exception e) {
                skipped++;
            }
        }
        return ApiResponse.ok(new BatchResult(success, skipped));
    }

    @RequirePermission("booking:approve")
    @PutMapping("/batch-reject")
    public ApiResponse<BatchResult> batchReject(@RequestBody @Valid BatchRejectRequest request) {
        if (request.getRemark() == null || request.getRemark().isBlank()) {
            throw new IllegalArgumentException("批量驳回时必须填写原因");
        }
        int success = 0;
        int skipped = 0;
        for (Long id : request.getBookingIds()) {
            try {
                bookingApplicationService.rejectBooking(id, request.getOperatorId(), request.getRemark());
                success++;
            } catch (Exception e) {
                skipped++;
            }
        }
        return ApiResponse.ok(new BatchResult(success, skipped));
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

    @Data
    public static class BatchRequest {
        @NotEmpty
        private List<Long> bookingIds;
        @NotNull
        private String operatorId;
        private String remark;
    }

    @Data
    public static class BatchRejectRequest {
        @NotEmpty
        private List<Long> bookingIds;
        @NotNull
        private String operatorId;
        @NotNull
        private String remark;
    }

    public record BatchResult(int successCount, int skippedCount) {}
}
