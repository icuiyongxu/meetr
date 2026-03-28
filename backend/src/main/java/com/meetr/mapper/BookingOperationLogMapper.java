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

    /** 查询指定时间窗口内的指定操作类型的 bookingId（用于提醒去重） */
    @org.apache.ibatis.annotations.Select("""
        select distinct booking_id
        from booking_operation_log
        where created_at_ms > #{windowStartMs}
          and created_at_ms <= #{nowMs}
          and operation_type = #{operationType}
        """)
    java.util.List<Long> findRemindedBookingIdsInWindow(
        @org.apache.ibatis.annotations.Param("windowStartMs") long windowStartMs,
        @org.apache.ibatis.annotations.Param("operationType") String operationType,
        @org.apache.ibatis.annotations.Param("nowMs") long nowMs);
}
