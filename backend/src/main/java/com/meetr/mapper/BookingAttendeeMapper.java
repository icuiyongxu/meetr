package com.meetr.mapper;

import com.meetr.domain.entity.BookingAttendee;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookingAttendeeMapper {

    @Select("""
        select id, booking_id, user_id, user_name, created_at_ms, updated_at_ms
        from booking_attendee
        where booking_id = #{bookingId}
        order by id asc
        """)
    List<BookingAttendee> findByBookingIdOrderByIdAsc(@Param("bookingId") Long bookingId);

    @Delete("delete from booking_attendee where booking_id = #{bookingId}")
    void deleteByBookingId(@Param("bookingId") Long bookingId);

    @Insert("""
        insert into booking_attendee (booking_id, user_id, user_name, created_at_ms, updated_at_ms)
        values (#{bookingId}, #{userId}, #{userName}, #{createdAtMs}, #{updatedAtMs})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BookingAttendee attendee);
}
