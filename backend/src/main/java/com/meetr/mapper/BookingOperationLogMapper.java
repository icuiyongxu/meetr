package com.meetr.mapper;

import com.meetr.domain.entity.BookingOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface BookingOperationLogMapper {

    @Insert("""
        insert into booking_operation_log (booking_id, operation_type, operator_id, operator_name, content, created_at_ms, updated_at_ms)
        values (#{bookingId}, #{operationType}, #{operatorId}, #{operatorName}, #{content}, #{createdAtMs}, #{updatedAtMs})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BookingOperationLog log);

    @org.apache.ibatis.annotations.Select("""
        select id, booking_id, operation_type, operator_id, operator_name, content, created_at_ms, updated_at_ms
        from booking_operation_log
        where booking_id = #{bookingId}
        order by created_at_ms asc
        """)
    java.util.List<BookingOperationLog> findByBookingId(@org.apache.ibatis.annotations.Param("bookingId") Long bookingId);
}
