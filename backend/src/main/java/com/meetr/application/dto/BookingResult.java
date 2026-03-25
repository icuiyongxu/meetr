package com.meetr.application.dto;

import com.meetr.domain.vo.RuleViolation;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookingResult {

    private boolean success;
    private BookingDTO booking;
    private List<RuleViolation> violations = new ArrayList<>();
    private List<BookingConflictDTO> conflicts = new ArrayList<>();

    public static BookingResult success(BookingDTO booking) {
        BookingResult result = new BookingResult();
        result.setSuccess(true);
        result.setBooking(booking);
        return result;
    }

    public static BookingResult rejected(List<RuleViolation> violations) {
        BookingResult result = new BookingResult();
        result.setSuccess(false);
        result.setViolations(violations);
        return result;
    }

    public static BookingResult conflicted(List<BookingConflictDTO> conflicts) {
        BookingResult result = new BookingResult();
        result.setSuccess(false);
        result.setConflicts(conflicts);
        return result;
    }
}
