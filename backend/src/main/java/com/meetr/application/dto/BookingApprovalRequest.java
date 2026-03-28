package com.meetr.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingApprovalRequest {
    @NotBlank
    private String operatorId;
    private String remark;
}
