package com.meetr.application.dto;

import java.util.List;

public record KioskResponse(
    KioskRoomDto room,
    String date,
    List<KioskBookingDto> bookings
) {}
