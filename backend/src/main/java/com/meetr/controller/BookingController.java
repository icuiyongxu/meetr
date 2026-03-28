package com.meetr.controller;

import com.meetr.application.BookingApplicationService;
import com.meetr.application.dto.CancelSeriesRequest;
import com.meetr.application.dto.UpdateSeriesRequest;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.BookingDetailDTO;
import com.meetr.application.dto.BookingResult;
import com.meetr.application.dto.BookingSearchRequest;
import com.meetr.application.dto.ConflictCheckRequest;
import com.meetr.application.dto.ConflictCheckResponse;
import com.meetr.application.dto.CreateBookingCommand;
import com.meetr.application.dto.SeriesBookingResponse;
import com.meetr.application.dto.UpdateBookingCommand;
import com.meetr.application.dto.UpdateFutureSeriesRequest;
import com.meetr.config.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingApplicationService bookingApplicationService;

    @RequirePermission("booking:manage")
    @PostMapping
    public ApiResponse<BookingResult> create(@Valid @RequestBody CreateBookingCommand command) {
        return ApiResponse.ok(bookingApplicationService.create(command));
    }

    @RequirePermission("booking:manage")
    @PutMapping("/{id}")
    public ApiResponse<BookingResult> update(@PathVariable Long id, @Valid @RequestBody UpdateBookingCommand command) {
        command.setBookingId(id);
        return ApiResponse.ok(bookingApplicationService.update(command));
    }

    @RequirePermission("booking:manage")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, @Valid @RequestBody CancelBookingRequest request) {
        bookingApplicationService.cancel(id, request.getOperatorId(), Boolean.TRUE.equals(request.getCancelSeries()));
        return ApiResponse.ok(null);
    }

    @RequirePermission("booking:view")
    @GetMapping("/{id}")
    public ApiResponse<BookingDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(bookingApplicationService.getById(id));
    }

    @RequirePermission("booking:view")
    @GetMapping("/{id}/detail")
    public ApiResponse<BookingDetailDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(bookingApplicationService.getDetail(id));
    }

    @RequirePermission("booking:view")
    @GetMapping("/mine")
    public ApiResponse<BookingApplicationService.PageResult<BookingDTO>> mine(
            @RequestParam String bookerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(bookingApplicationService.getMyBookings(bookerId, page, size));
    }

    @RequirePermission("booking:view")
    @GetMapping("/today")
    public ApiResponse<List<BookingDTO>> today(@RequestParam String bookerId) {
        return ApiResponse.ok(bookingApplicationService.getTodayBookings(bookerId));
    }

    @RequirePermission("booking:view")
    @GetMapping("/search")
    public ApiResponse<BookingApplicationService.PageResult<BookingDTO>> search(@ModelAttribute BookingSearchRequest request) {
        if (request.getSize() <= 0) request.setSize(10);
        if (request.getSize() > 100) request.setSize(100);
        return ApiResponse.ok(bookingApplicationService.searchBookings(request));
    }

    @RequirePermission("booking:manage")
    @PostMapping("/check-conflict")
    public ApiResponse<ConflictCheckResponse> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return ApiResponse.ok(bookingApplicationService.checkConflict(request));
    }

    @Data
    public static class CancelBookingRequest {
        @NotBlank
        private String operatorId;
        private Boolean cancelSeries;
    }

    /**
     * 获取某个预约所属系列的所有预约（主预约 + 子预约列表）。
     * 传入任意一次预约的 ID 即可。
     */
    @RequirePermission("booking:view")
    @GetMapping("/{id}/series")
    public ApiResponse<SeriesBookingResponse> getSeriesBookings(
            @PathVariable Long id,
            @RequestParam String bookerId) {
        return ApiResponse.ok(bookingApplicationService.getSeriesBookings(id, bookerId));
    }

    /**
     * 批量修改系列中从指定序号开始的未来所有预约的时间。
     * 仅修改时间字段，不改变其他内容。
     */
    @RequirePermission("booking:manage")
    @PutMapping("/{id}/future")
    public ApiResponse<SeriesBookingResponse> updateFutureSeries(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFutureSeriesRequest request) {
        return ApiResponse.ok(
            bookingApplicationService.updateFutureSeries(id, request.getOperatorId(), request));
    }

    /**
     * 统一修改系列预约（scope=ONCE / FUTURE / ALL）。
     */
    @RequirePermission("booking:manage")
    @PutMapping("/{id}/series")
    public ApiResponse<BookingResult> updateSeries(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSeriesRequest request) {
        return ApiResponse.ok(bookingApplicationService.updateSeries(id, request));
    }

    /**
     * 统一取消系列预约（scope=ONCE / FUTURE / ALL）。
     */
    @RequirePermission("booking:manage")
    @PostMapping("/{id}/series-cancel")
    public ApiResponse<SeriesBookingResponse> cancelSeries(
            @PathVariable Long id,
            @Valid @RequestBody CancelSeriesRequest request) {
        return ApiResponse.ok(bookingApplicationService.cancelSeries(id, request));
    }
}
