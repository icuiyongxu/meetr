package com.meetr.domain.repository;

import com.meetr.domain.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        select b from Booking b
        where b.roomId = :roomId
          and b.status <> com.meetr.domain.enums.BookingStatus.CANCELED
          and b.approvalStatus <> com.meetr.domain.enums.ApprovalStatus.REJECTED
          and b.startTimeMs < :newEndMs
          and b.endTimeMs > :newStartMs
          and (:excludeBookingId is null or b.id <> :excludeBookingId)
        order by b.startTimeMs asc
        """)
    List<Booking> findConflicting(@Param("roomId") Long roomId,
                                  @Param("newStartMs") Long newStartMs,
                                  @Param("newEndMs") Long newEndMs,
                                  @Param("excludeBookingId") Long excludeBookingId);

    Page<Booking> findByBookerIdOrderByStartTimeMsDesc(String bookerId, Pageable pageable);

    @Query("""
        select b from Booking b
        where b.bookerId = :bookerId
          and b.startTimeMs >= :dayStartMs
          and b.startTimeMs < :dayEndMs
          and b.status <> com.meetr.domain.enums.BookingStatus.CANCELED
        order by b.startTimeMs asc
        """)
    List<Booking> findTodayBookings(@Param("bookerId") String bookerId,
                                    @Param("dayStartMs") Long dayStartMs,
                                    @Param("dayEndMs") Long dayEndMs);

    @Query("""
        select count(b) from Booking b
        where b.bookerId = :bookerId
          and b.status <> com.meetr.domain.enums.BookingStatus.CANCELED
          and b.approvalStatus <> com.meetr.domain.enums.ApprovalStatus.REJECTED
          and b.startTimeMs >= :dayStartMs
          and b.startTimeMs < :dayEndMs
          and (:excludeBookingId is null or b.id <> :excludeBookingId)
        """)
    long countActiveBookingsOnDay(@Param("bookerId") String bookerId,
                                  @Param("dayStartMs") Long dayStartMs,
                                  @Param("dayEndMs") Long dayEndMs,
                                  @Param("excludeBookingId") Long excludeBookingId);

    /** 查询指定会议室指定日期的所有预约（供日历视图使用） */
    @Query("""
        select b from Booking b
        where b.roomId = :roomId
          and b.startTimeMs < :dayEndMs
          and b.endTimeMs >= :dayStartMs
        order by b.startTimeMs asc
        """)
    List<Booking> findByRoomIdAndDate(@Param("roomId") Long roomId,
                                        @Param("dayStartMs") Long dayStartMs,
                                        @Param("dayEndMs") Long dayEndMs);

    /**
     * 查询系列所有子预约（不含主预约自身）。
     * @param seriesId 主预约 ID
     */
    List<Booking> findByParentIdOrderBySeriesIndexAsc(Long seriesId);

    @Query("""
        select b from Booking b
        where (:bookerId is null or b.bookerId = :bookerId)
          and (:keyword is null or lower(b.subject) like lower(concat('%', :keyword, '%')))
          and (:roomId is null or b.roomId = :roomId)
          and (:status is null or b.status = :status)
          and (:startTimeFrom is null or b.startTimeMs >= :startTimeFrom)
          and (:startTimeTo is null or b.startTimeMs <= :startTimeTo)
        order by b.startTimeMs desc
        """)
    Page<Booking> searchBookings(
        @Param("bookerId") String bookerId,
        @Param("keyword") String keyword,
        @Param("roomId") Long roomId,
        @Param("status") String status,
        @Param("startTimeFrom") Long startTimeFrom,
        @Param("startTimeTo") Long startTimeTo,
        Pageable pageable
    );
}
