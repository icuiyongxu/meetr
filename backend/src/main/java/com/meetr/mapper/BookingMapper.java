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
}
