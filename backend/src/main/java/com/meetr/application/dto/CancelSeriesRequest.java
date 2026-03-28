package com.meetr.application.dto;

import com.meetr.domain.enums.SeriesScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelSeriesRequest {

    @NotBlank
    private String operatorId;

    @NotNull
    private SeriesScope scope;
}
