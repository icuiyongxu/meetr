package com.meetr.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRoomRequest {

    @NotBlank
    private String operatorId;

    @NotNull
    private Long newRoomId;
}
