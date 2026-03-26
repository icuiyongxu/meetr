package com.meetr.mapper;

import com.meetr.domain.entity.RoomConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RoomConfigMapper {

    @Select("""
        select id, room_id, resolution, default_duration, morning_starts, evening_ends,
               min_book_ahead_minutes, max_book_ahead_days, min_duration_minutes, max_duration_minutes,
               max_per_day, max_per_week, approval_required, status, admin_user_ids, created_at_ms, updated_at_ms
        from room_config
        where room_id = #{roomId}
        order by id asc
        limit 1
        """)
    RoomConfig findFirstByRoomId(Long roomId);

    @Select("""
        select id, room_id, resolution, default_duration, morning_starts, evening_ends,
               min_book_ahead_minutes, max_book_ahead_days, min_duration_minutes, max_duration_minutes,
               max_per_day, max_per_week, approval_required, status, admin_user_ids, created_at_ms, updated_at_ms
        from room_config
        where room_id is null
        order by id asc
        limit 1
        """)
    RoomConfig findFirstByRoomIdIsNull();

    @Insert("""
        insert into room_config (
            room_id, resolution, default_duration, morning_starts, evening_ends,
            min_book_ahead_minutes, max_book_ahead_days, min_duration_minutes, max_duration_minutes,
            max_per_day, max_per_week, approval_required, status, admin_user_ids, created_at_ms, updated_at_ms
        ) values (
            #{roomId}, #{resolution}, #{defaultDuration}, #{morningStarts}, #{eveningEnds},
            #{minBookAheadMinutes}, #{maxBookAheadDays}, #{minDurationMinutes}, #{maxDurationMinutes},
            #{maxPerDay}, #{maxPerWeek}, #{approvalRequired}, #{status}, #{adminUserIds}, #{createdAtMs}, #{updatedAtMs}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoomConfig config);

    @Update("""
        update room_config
        set room_id = #{roomId},
            resolution = #{resolution},
            default_duration = #{defaultDuration},
            morning_starts = #{morningStarts},
            evening_ends = #{eveningEnds},
            min_book_ahead_minutes = #{minBookAheadMinutes},
            max_book_ahead_days = #{maxBookAheadDays},
            min_duration_minutes = #{minDurationMinutes},
            max_duration_minutes = #{maxDurationMinutes},
            max_per_day = #{maxPerDay},
            max_per_week = #{maxPerWeek},
            approval_required = #{approvalRequired},
            status = #{status},
            admin_user_ids = #{adminUserIds},
            updated_at_ms = #{updatedAtMs}
        where id = #{id}
        """)
    int update(RoomConfig config);
}
