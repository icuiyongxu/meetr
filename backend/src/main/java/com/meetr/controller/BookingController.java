package com.meetr.controller;

import com.meetr.application.BookingApplicationService;
import com.meetr.application.dto.BookingDTO;
import com.meetr.application.dto.BookingResult;
import com.meetr.application.dto.ConflictCheckRequest;
import com.meetr.application.dto.ConflictCheckResponse;
import com.meetr.application.dto.CreateBookingCommand;
import com.meetr.application.dto.UpdateBookingCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingApplicationService bookingApplicationService;

    /**
     * 查询指定会议室指定日期的全部预约（供日历视图渲染）
     * GET /api/bookings/room/{roomId}/date/2026-03-25
     */
    @GetMapping("/room/{roomId}/date/{date}")
    public ApiResponse<List<BookingDTO>> getByRoomAndDate(
            @PathVariable Long roomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(bookingApplicationService.getBookingsByRoomAndDate(roomId, date));
    }

    @PostMapping
    public ApiResponse<BookingResult> create(@Valid @RequestBody CreateBookingCommand command) {
        return ApiResponse.ok(bookingApplicationService.create(command));
    }

    @PutMapping("/{id}")
    public ApiResponse<BookingResult> update(@PathVariable Long id, @Valid @RequestBody UpdateBookingCommand command) {
        command.setBookingId(id);
        return ApiResponse.ok(bookingApplicationService.update(command));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, @Valid @RequestBody CancelBookingRequest request) {
        bookingApplicationService.cancel(id, request.getOperatorId());
        return ApiResponse.ok(null);
    }

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

    @PostMapping("/check-conflict")
    public ApiResponse<ConflictCheckResponse> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return ApiResponse.ok(bookingApplicationService.checkConflict(request));
    }

    @Data
    public static class CancelBookingRequest {

        @NotBlank
        private String operatorId;
    }
}
