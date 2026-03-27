package com.meetr.mapper;

import com.github.pagehelper.Page;
import com.meetr.domain.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BookingMapper {

    @Select("""
        select id, room_id, subject, booker_id, booker_name, start_time_ms, end_time_ms,
               attendee_count, status, approval_status, remark, version, recurrence_type,
               recurrence_end_date, parent_id, series_index, created_at_ms, updated_at_ms
        from booking
        where id = #{id}
        """)
    Booking findById(@Param("id") Long id);

    int insert(Booking booking);

    @Update("""
        update booking
        set room_id = #{roomId},
            subject = #{subject},
            booker_id = #{bookerId},
            booker_name = #{bookerName},
            start_time_ms = #{startTimeMs},
            end_time_ms = #{endTimeMs},
            attendee_count = #{attendeeCount},
            status = #{status},
            approval_status = #{approvalStatus},
            remark = #{remark},
            version = #{version},
            recurrence_type = #{recurrenceType},
            recurrence_end_date = #{recurrenceEndDate},
            parent_id = #{parentId},
            series_index = #{seriesIndex},
            updated_at_ms = #{updatedAtMs}
        where id = #{id}
        """)
    int update(Booking booking);

    List<Booking> findConflicting(@Param("roomId") Long roomId,
                                  @Param("newStartMs") Long newStartMs,
                                  @Param("newEndMs") Long newEndMs,
                                  @Param("excludeBookingId") Long excludeBookingId);

    Page<Booking> findByBookerIdOrderByStartTimeMsDesc(@Param("bookerId") String bookerId);

    List<Booking> findTodayBookings(@Param("bookerId") String bookerId,
                                    @Param("dayStartMs") Long dayStartMs,
                                    @Param("dayEndMs") Long dayEndMs);

    long countActiveBookingsOnDay(@Param("bookerId") String bookerId,
                                  @Param("dayStartMs") Long dayStartMs,
                                  @Param("dayEndMs") Long dayEndMs,
                                  @Param("excludeBookingId") Long excludeBookingId);

    List<Booking> findByRoomIdAndDate(@Param("roomId") Long roomId,
                                      @Param("dayStartMs") Long dayStartMs,
                                      @Param("dayEndMs") Long dayEndMs);

    @Select("""
        select id, room_id, subject, booker_id, booker_name, start_time_ms, end_time_ms,
               attendee_count, status, approval_status, remark, version, recurrence_type,
               recurrence_end_date, parent_id, series_index, created_at_ms, updated_at_ms
        from booking
        where parent_id = #{seriesId}
        order by series_index asc
        """)
    List<Booking> findByParentIdOrderBySeriesIndexAsc(@Param("seriesId") Long seriesId);

    Page<Booking> searchBookings(@Param("bookerId") String bookerId,
                                 @Param("keyword") String keyword,
                                 @Param("roomId") Long roomId,
                                 @Param("status") String status,
                                 @Param("startTimeFrom") Long startTimeFrom,
                                 @Param("startTimeTo") Long startTimeTo);

    /**
     * 找到系列所有预约（包含 master），按 seriesIndex 升序。
     * 如果传入的是子预约ID，会先查出 masterId 再查全系列。
     */
    @Select("""
        SELECT b.id, b.room_id, b.subject, b.booker_id, b.booker_name, b.start_time_ms,
               b.end_time_ms, b.attendee_count, b.status, b.approval_status, b.remark,
               b.version, b.recurrence_type, b.recurrence_end_date, b.parent_id,
               b.series_index, b.created_at_ms, b.updated_at_ms
        FROM booking b
        WHERE b.series_index = 1
          AND b.recurrence_type != 'NONE'
          AND b.parent_id IS NULL
          AND b.booker_id = #{bookerId}
          AND b.id = (
              SELECT COALESCE(parent_id, id) FROM booking WHERE id = #{bookingId}
          )
        UNION ALL
        SELECT b.id, b.room_id, b.subject, b.booker_id, b.booker_name, b.start_time_ms,
               b.end_time_ms, b.attendee_count, b.status, b.approval_status, b.remark,
               b.version, b.recurrence_type, b.recurrence_end_date, b.parent_id,
               b.series_index, b.created_at_ms, b.updated_at_ms
        FROM booking b
        WHERE b.parent_id = (
            SELECT COALESCE(parent_id, id) FROM booking WHERE id = #{bookingId}
        )
        ORDER BY series_index ASC
        """)
    List<Booking> findSeriesBookings(@Param("bookingId") Long bookingId,
                                    @Param("bookerId") String bookerId);

    /**
     * 批量更新系列中从指定 seriesIndex 开始的所有预约的开始和结束时间。
     * 时长保持与原时间差一致。
     */
    @Update("""
        UPDATE booking
        SET start_time_ms = #{newStartMs},
            end_time_ms   = #{newEndMs},
            updated_at_ms = UNIX_TIMESTAMP(NOW(3)) * 1000
        WHERE parent_id = #{seriesId}
          AND series_index >= #{fromSeriesIndex}
          AND status != 'CANCELED'
        """)
    int updateFutureSeries(@Param("seriesId") Long seriesId,
                           @Param("fromSeriesIndex") Integer fromSeriesIndex,
                           @Param("newStartMs") Long newStartMs,
                           @Param("newEndMs") Long newEndMs);
}
