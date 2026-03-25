package com.meetr.domain.repository;

import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {

    boolean existsByBuildingIdAndNameIgnoreCase(Long buildingId, String name);

    boolean existsByBuildingIdAndNameIgnoreCaseAndIdNot(Long buildingId, String name, Long id);

    @Query("""
        select r from MeetingRoom r
        where (:buildingId is null or r.buildingId = :buildingId)
          and (:floor is null or r.floor = :floor)
          and (:capacity is null or r.capacity >= :capacity)
          and (:status is null or r.status = :status)
          and (
                :keyword is null
                or lower(r.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(r.floor, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(r.remark, '')) like lower(concat('%', :keyword, '%'))
          )
        order by r.buildingId asc, r.floor asc, r.name asc
        """)
    List<MeetingRoom> search(@Param("buildingId") Long buildingId,
                             @Param("floor") String floor,
                             @Param("capacity") Integer capacity,
                             @Param("status") RoomStatus status,
                             @Param("keyword") String keyword);

    @Query("""
        select r from MeetingRoom r
        where r.status = com.meetr.domain.enums.RoomStatus.ENABLED
          and (:buildingId is null or r.buildingId = :buildingId)
          and (:capacity is null or r.capacity >= :capacity)
          and not exists (
                select b.id from Booking b
                where b.roomId = r.id
                  and b.status <> com.meetr.domain.enums.BookingStatus.CANCELED
                  and b.approvalStatus <> com.meetr.domain.enums.ApprovalStatus.REJECTED
                  and b.startTimeMs < :endTimeMs
                  and b.endTimeMs > :startTimeMs
          )
        order by r.buildingId asc, r.floor asc, r.name asc
        """)
    List<MeetingRoom> findAvailable(@Param("startTimeMs") Long startTimeMs,
                                    @Param("endTimeMs") Long endTimeMs,
                                    @Param("buildingId") Long buildingId,
                                    @Param("capacity") Integer capacity);
}
