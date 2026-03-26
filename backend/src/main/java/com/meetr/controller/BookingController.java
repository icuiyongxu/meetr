package com.meetr.controller;

import com.meetr.application.BookingApplicationService;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.BookingResult;
import com.meetr.application.dto.BookingSearchRequest;
import com.meetr.application.dto.ConflictCheckRequest;
import com.meetr.application.dto.ConflictCheckResponse;
import com.meetr.application.dto.CreateBookingCommand;
import com.meetr.application.dto.UpdateBookingCommand;
import com.meetr.config.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping("/mine")
    public ApiResponse<Page<BookingDTO>> mine(@RequestParam String bookerId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(bookingApplicationService.getMyBookings(bookerId, PageRequest.of(page, size)));
    }

    @GetMapping("/today")
    public ApiResponse<List<BookingDTO>> today(@RequestParam String bookerId) {
        return ApiResponse.ok(bookingApplicationService.getTodayBookings(bookerId));
    }

    @RequirePermission("booking:view")
    @GetMapping("/search")
    public ApiResponse<Page<BookingDTO>> search(@ModelAttribute BookingSearchRequest request) {
        if (request.getSize() <= 0) request.setSize(10);
        if (request.getSize() > 100) request.setSize(100);
        return ApiResponse.ok(bookingApplicationService.searchBookings(request));
    }

    @PostMapping("/check-conflict")
    public ApiResponse<ConflictCheckResponse> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return ApiResponse.ok(bookingApplicationService.checkConflict(request));
    }

    @Data
    public static class CancelBookingRequest {

        @NotBlank
        private String operatorId;

        /** 是否取消整个系列；默认 false 只取消当前 */
        private Boolean cancelSeries;
    }
}
