package com.meetr.mapper;

import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.enums.RoomStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MeetingRoomMapper {

    @Select("""
        select count(1) > 0
        from meeting_room
        where building_id = #{buildingId}
          and lower(name) = lower(#{name})
        """)
    boolean existsByBuildingIdAndNameIgnoreCase(@Param("buildingId") Long buildingId, @Param("name") String name);

    @Select("""
        select count(1) > 0
        from meeting_room
        where building_id = #{buildingId}
          and lower(name) = lower(#{name})
          and id <> #{id}
        """)
    boolean existsByBuildingIdAndNameIgnoreCaseAndIdNot(@Param("buildingId") Long buildingId, @Param("name") String name, @Param("id") Long id);

    @Select("select count(1) > 0 from meeting_room where id = #{id}")
    boolean existsById(@Param("id") Long id);

    @Select("""
        select id, building_id, name, floor, capacity, equipment, status, remark, created_at_ms, updated_at_ms
        from meeting_room
        where id = #{id}
        """)
    MeetingRoom findById(@Param("id") Long id);

    @Insert("""
        insert into meeting_room (building_id, name, floor, capacity, equipment, status, remark, created_at_ms, updated_at_ms)
        values (#{buildingId}, #{name}, #{floor}, #{capacity}, #{equipment}, #{status}, #{remark}, #{createdAtMs}, #{updatedAtMs})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MeetingRoom room);

    @Update("""
        update meeting_room
        set building_id = #{buildingId},
            name = #{name},
            floor = #{floor},
            capacity = #{capacity},
            equipment = #{equipment},
            status = #{status},
            remark = #{remark},
            updated_at_ms = #{updatedAtMs}
        where id = #{id}
        """)
    int update(MeetingRoom room);

    List<MeetingRoom> search(@Param("buildingId") Long buildingId,
                             @Param("floor") String floor,
                             @Param("capacity") Integer capacity,
                             @Param("status") RoomStatus status,
                             @Param("keyword") String keyword);

    List<MeetingRoom> findAvailable(@Param("startTimeMs") Long startTimeMs,
                                    @Param("endTimeMs") Long endTimeMs,
                                    @Param("buildingId") Long buildingId,
                                    @Param("capacity") Integer capacity);
}
