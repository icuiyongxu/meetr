package com.meetr.exception;

import com.meetr.application.dto.BookingConflictDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class ConflictException extends BusinessException {

    private final List<BookingConflictDTO> conflicts;

    public ConflictException(String message, List<BookingConflictDTO> conflicts) {
        super(40901, message);
        this.conflicts = conflicts;
    }
}
