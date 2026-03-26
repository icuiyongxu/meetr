package com.meetr.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookingOperationLog extends BaseEntity {

    private Long id;
    private Long bookingId;
    private String operationType;
    private String operatorId;
    private String operatorName;
    private String content;
}
