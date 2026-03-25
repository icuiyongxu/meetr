package com.meetr.domain.repository;

import com.meetr.domain.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        select b from Booking b
        where b.roomId = :roomId
          and b.status <> com.meetr.domain.enums.BookingStatus.CANCELED
          and b.approvalStatus <> com.meetr.domain.enums.ApprovalStatus.REJECTED
          and b.startTime < :newEnd
          and b.endTime > :newStart
          and (:excludeBookingId is null or b.id <> :excludeBookingId)
        order by b.startTime asc
        """)
    List<Booking> findConflicting(@Param("roomId") Long roomId,
                                  @Param("newStart") LocalDateTime newStart,
                                  @Param("newEnd") LocalDateTime newEnd,
                                  @Param("excludeBookingId") Long excludeBookingId);

    Page<Booking> findByBookerIdOrderByStartTimeDesc(String bookerId, Pageable pageable);

    @Query("""
        select b from Booking b
        where b.bookerId = :bookerId
          and b.startTime >= :dayStart
          and b.startTime < :dayEnd
          and b.status <> com.meetr.domain.enums.BookingStatus.CANCELED
        order by b.startTime asc
        """)
    List<Booking> findTodayBookings(@Param("bookerId") String bookerId,
                                    @Param("dayStart") LocalDateTime dayStart,
                                    @Param("dayEnd") LocalDateTime dayEnd);

    @Query("""
        select count(b) from Booking b
        where b.bookerId = :bookerId
          and b.status <> com.meetr.domain.enums.BookingStatus.CANCELED
          and b.approvalStatus <> com.meetr.domain.enums.ApprovalStatus.REJECTED
          and b.startTime >= :dayStart
          and b.startTime < :dayEnd
          and (:excludeBookingId is null or b.id <> :excludeBookingId)
        """)
    long countActiveBookingsOnDay(@Param("bookerId") String bookerId,
                                  @Param("dayStart") LocalDateTime dayStart,
                                  @Param("dayEnd") LocalDateTime dayEnd,
                                  @Param("excludeBookingId") Long excludeBookingId);

    /** 查询指定会议室指定日期的所有预约（供日历视图使用，含已取消的用于展示） */
    @Query("""
        select b from Booking b
        where b.roomId = :roomId
          and b.startTime < :dayEnd
          and b.endTime >= :dayStart
        order by b.startTime asc
        """)
    List<Booking> findByRoomIdAndDate(@Param("roomId") Long roomId,
                                        @Param("dayStart") LocalDateTime dayStart,
                                        @Param("dayEnd") LocalDateTime dayEnd);
}
