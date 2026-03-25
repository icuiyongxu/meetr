package com.meetr.application.dto;

import com.meetr.domain.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomStatusUpdateRequest {

    @NotNull
    private RoomStatus status;
}
