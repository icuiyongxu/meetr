package com.meetr.application;

import com.meetr.application.dto.RoomDTO;
import com.meetr.application.dto.RoomStatusUpdateRequest;
import com.meetr.application.dto.RoomUpsertRequest;
import com.meetr.domain.entity.Building;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.enums.BuildingStatus;
import com.meetr.domain.repository.BuildingRepository;
import com.meetr.domain.repository.MeetingRoomRepository;
import com.meetr.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomApplicationService {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final MeetingRoomRepository meetingRoomRepository;
    private final BuildingRepository buildingRepository;

    public List<RoomDTO> list(Long buildingId, String floor, Integer capacity, com.meetr.domain.enums.RoomStatus status, String keyword) {
        return enrich(meetingRoomRepository.search(buildingId, normalize(floor), capacity, status, normalize(keyword)));
    }

    public List<RoomDTO> available(LocalDateTime startTime, LocalDateTime endTime, Long buildingId, Integer capacity) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException(40002, "开始时间必须早于结束时间");
        }
        // Convert LocalDateTime (Beijing) to UTC millis
        long startMs = startTime.atZone(BEIJING).toInstant().toEpochMilli();
        long endMs = endTime.atZone(BEIJING).toInstant().toEpochMilli();
        return enrich(meetingRoomRepository.findAvailable(startMs, endMs, buildingId, capacity)).stream()
            .filter(room -> {
                Building building = buildingRepository.findById(room.getBuildingId()).orElse(null);
                return building != null && building.getStatus() == BuildingStatus.ACTIVE;
            })
            .toList();
    }

    public RoomDTO getById(Long id) {
        MeetingRoom room = meetingRoomRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "会议室不存在"));
        return toDto(room, getBuildingMap(List.of(room)).get(room.getBuildingId()));
    }

    @Transactional
    public RoomDTO create(RoomUpsertRequest request) {
        Building building = requireBuilding(request.getBuildingId());
        if (meetingRoomRepository.existsByBuildingIdAndNameIgnoreCase(request.getBuildingId(), request.getName())) {
            throw new BusinessException(40002, "同一楼栋下会议室名称不能重复");
        }
        MeetingRoom room = new MeetingRoom();
        apply(room, request);
        return toDto(meetingRoomRepository.save(room), building);
    }

    @Transactional
    public RoomDTO update(Long id, RoomUpsertRequest request) {
        Building building = requireBuilding(request.getBuildingId());
        MeetingRoom room = meetingRoomRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "会议室不存在"));
        if (meetingRoomRepository.existsByBuildingIdAndNameIgnoreCaseAndIdNot(request.getBuildingId(), request.getName(), id)) {
            throw new BusinessException(40002, "同一楼栋下会议室名称不能重复");
        }
        apply(room, request);
        return toDto(meetingRoomRepository.save(room), building);
    }

    @Transactional
    public RoomDTO updateStatus(Long id, RoomStatusUpdateRequest request) {
        MeetingRoom room = meetingRoomRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "会议室不存在"));
        room.setStatus(request.getStatus());
        return toDto(meetingRoomRepository.save(room), requireBuilding(room.getBuildingId()));
    }

    private List<RoomDTO> enrich(List<MeetingRoom> rooms) {
        Map<Long, Building> buildingMap = getBuildingMap(rooms);
        return rooms.stream()
            .map(room -> toDto(room, buildingMap.get(room.getBuildingId())))
            .toList();
    }

    private Map<Long, Building> getBuildingMap(List<MeetingRoom> rooms) {
        List<Long> buildingIds = rooms.stream().map(MeetingRoom::getBuildingId).distinct().toList();
        Map<Long, Building> map = new HashMap<>();
        buildingRepository.findAllById(buildingIds).forEach(building -> map.put(building.getId(), building));
        return map;
    }

    private void apply(MeetingRoom room, RoomUpsertRequest request) {
        room.setBuildingId(request.getBuildingId());
        room.setName(request.getName());
        room.setFloor(request.getFloor());
        room.setCapacity(request.getCapacity() == null ? 0 : request.getCapacity());
        room.setEquipmentItems(request.getEquipmentItems());
        room.setStatus(request.getStatus());
        room.setRemark(request.getRemark());
    }

    private RoomDTO toDto(MeetingRoom room, Building building) {
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setBuildingId(room.getBuildingId());
        dto.setBuildingName(building == null ? null : building.getName());
        dto.setName(room.getName());
        dto.setFloor(room.getFloor());
        dto.setCapacity(room.getCapacity());
        dto.setEquipment(room.getEquipment());
        dto.setEquipmentItems(room.equipmentItems());
        dto.setStatus(room.getStatus());
        dto.setRemark(room.getRemark());
        return dto;
    }

    private Building requireBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId)
            .orElseThrow(() -> new BusinessException(40001, "楼栋不存在"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
