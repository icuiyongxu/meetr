package com.meetr.domain.repository;

import com.meetr.domain.entity.BookingAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingAttendeeRepository extends JpaRepository<BookingAttendee, Long> {

    List<BookingAttendee> findByBookingIdOrderByIdAsc(Long bookingId);

    void deleteByBookingId(Long bookingId);
}
