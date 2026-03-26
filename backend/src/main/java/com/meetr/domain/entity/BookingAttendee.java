package com.meetr.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookingAttendee extends BaseEntity {

    private Long id;
    private Long bookingId;
    private String userId;
    private String userName;
}
