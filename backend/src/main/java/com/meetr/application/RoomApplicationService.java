package com.meetr.application;

import com.meetr.application.dto.RoomDTO;
import com.meetr.application.dto.RoomStatusUpdateRequest;
import com.meetr.application.dto.RoomUpsertRequest;
import com.meetr.domain.entity.Building;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.enums.BuildingStatus;
import com.meetr.exception.BusinessException;
import com.meetr.mapper.BuildingMapper;
import com.meetr.mapper.MeetingRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomApplicationService {

    private final MeetingRoomMapper meetingRoomMapper;
    private final BuildingMapper buildingMapper;

    public List<RoomDTO> list(Long buildingId, String floor, Integer capacity, com.meetr.domain.enums.RoomStatus status, String keyword) {
        return enrich(meetingRoomMapper.search(buildingId, normalize(floor), capacity, status, normalize(keyword)));
    }

    public List<RoomDTO> available(Long startTimeMs, Long endTimeMs, Long buildingId, Integer capacity) {
        if (startTimeMs == null || endTimeMs == null || startTimeMs >= endTimeMs) {
            throw new BusinessException(40002, "开始时间必须早于结束时间");
        }
        return enrich(meetingRoomMapper.findAvailable(startTimeMs, endTimeMs, buildingId, capacity)).stream()
            .filter(room -> {
                Building building = buildingMapper.findById(room.getBuildingId());
                return building != null && building.getStatus() == BuildingStatus.ACTIVE;
            })
            .toList();
    }

    public RoomDTO getById(Long id) {
        MeetingRoom room = meetingRoomMapper.findById(id);
        if (room == null) {
            throw new BusinessException(40001, "会议室不存在");
        }
        return toDto(room, getBuildingMap(List.of(room)).get(room.getBuildingId()));
    }

    @Transactional
    public RoomDTO create(RoomUpsertRequest request) {
        Building building = requireBuilding(request.getBuildingId());
        if (meetingRoomMapper.existsByBuildingIdAndNameIgnoreCase(request.getBuildingId(), request.getName())) {
            throw new BusinessException(40002, "同一楼栋下会议室名称不能重复");
        }
        MeetingRoom room = new MeetingRoom();
        apply(room, request);
        room.initTimestampsForInsert();
        meetingRoomMapper.insert(room);
        return toDto(room, building);
    }

    @Transactional
    public RoomDTO update(Long id, RoomUpsertRequest request) {
        Building building = requireBuilding(request.getBuildingId());
        MeetingRoom room = meetingRoomMapper.findById(id);
        if (room == null) {
            throw new BusinessException(40001, "会议室不存在");
        }
        if (meetingRoomMapper.existsByBuildingIdAndNameIgnoreCaseAndIdNot(request.getBuildingId(), request.getName(), id)) {
            throw new BusinessException(40002, "同一楼栋下会议室名称不能重复");
        }
        apply(room, request);
        room.touchForUpdate();
        meetingRoomMapper.update(room);
        return toDto(room, building);
    }

    @Transactional
    public RoomDTO updateStatus(Long id, RoomStatusUpdateRequest request) {
        MeetingRoom room = meetingRoomMapper.findById(id);
        if (room == null) {
            throw new BusinessException(40001, "会议室不存在");
        }
        room.setStatus(request.getStatus());
        room.touchForUpdate();
        meetingRoomMapper.update(room);
        return toDto(room, requireBuilding(room.getBuildingId()));
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
        if (!buildingIds.isEmpty()) {
            buildingMapper.findAllByIds(buildingIds).forEach(building -> map.put(building.getId(), building));
        }
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
        Building building = buildingMapper.findById(buildingId);
        if (building == null) {
            throw new BusinessException(40001, "楼栋不存在");
        }
        return building;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
