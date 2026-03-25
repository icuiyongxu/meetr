package com.meetr.domain.repository;

import com.meetr.domain.entity.RoomConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomConfigRepository extends JpaRepository<RoomConfig, Long> {

    Optional<RoomConfig> findFirstByRoomId(Long roomId);

    Optional<RoomConfig> findFirstByRoomIdIsNull();
}
