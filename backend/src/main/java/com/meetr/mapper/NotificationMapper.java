package com.meetr.mapper;

import com.github.pagehelper.Page;
import com.meetr.domain.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("""
        INSERT INTO notification (user_id, event_type, title, content, booking_id, room_id, is_read, created_at_ms)
        VALUES (#{userId}, #{eventType}, #{title}, #{content}, #{bookingId}, #{roomId}, 0, #{createdAtMs})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Select("""
        SELECT id, user_id, event_type, title, content, booking_id, room_id,
               booking_start_time_ms, booking_end_time_ms,
               is_read, read_at, created_at_ms
        FROM notification
        WHERE user_id = #{userId}
        ORDER BY created_at_ms DESC
        LIMIT #{limit}
        OFFSET #{offset}
        """)
    List<Notification> findByUserIdPaged(@Param("userId") String userId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") String userId);

    @Select("""
        SELECT id, user_id, event_type, title, content, booking_id, room_id,
               booking_start_time_ms, booking_end_time_ms,
               is_read, read_at, created_at_ms
        FROM notification
        WHERE id = #{id}
        """)
    Notification findById(@Param("id") Long id);

    @Update("""
        UPDATE notification
        SET is_read = 1, read_at = #{readAt}
        WHERE id = #{id}
        """)
    int markAsRead(@Param("id") Long id, @Param("readAt") Long readAt);

    @Update("""
        UPDATE notification
        SET is_read = 1, read_at = #{readAt}
        WHERE user_id = #{userId} AND is_read = 0
        """)
    int markAllAsRead(@Param("userId") String userId, @Param("readAt") Long readAt);

    @Delete("DELETE FROM notification WHERE created_at_ms < #{thresholdMs}")
    int deleteOlderThan(@Param("thresholdMs") Long thresholdMs);
}
