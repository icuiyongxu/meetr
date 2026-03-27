package com.meetr.mapper;

import com.meetr.domain.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KioskMapper {

    @Select("""
        SELECT b.id, b.room_id, b.subject, b.booker_id, b.booker_name,
               b.start_time_ms, b.end_time_ms, b.attendee_count, b.status,
               b.approval_status, b.remark, b.version, b.recurrence_type,
               b.recurrence_end_date, b.parent_id, b.series_index,
               b.created_at_ms, b.updated_at_ms,
               r.name as room_name, bl.id as building_id, bl.name as building_name
        FROM booking b
        LEFT JOIN meeting_room r ON b.room_id = r.id
        LEFT JOIN building bl ON r.building_id = bl.id
        WHERE b.room_id = #{roomId}
          AND b.start_time_ms >= #{dayStartMs}
          AND b.start_time_ms < #{dayEndMs}
          AND b.status != 'CANCELED'
          AND b.approval_status IN ('NONE', 'APPROVED')
        ORDER BY b.start_time_ms ASC
        """)
    List<Booking> findByRoomAndDay(@Param("roomId") Long roomId,
                                  @Param("dayStartMs") Long dayStartMs,
                                  @Param("dayEndMs") Long dayEndMs);
}
