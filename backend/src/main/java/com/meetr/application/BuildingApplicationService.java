package com.meetr.application;

import com.meetr.application.dto.BuildingDTO;
import com.meetr.application.dto.BuildingUpsertRequest;
import com.meetr.domain.entity.Building;
import com.meetr.mapper.BuildingMapper;
import com.meetr.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingApplicationService {

    private final BuildingMapper buildingMapper;

    public List<BuildingDTO> list() {
        return buildingMapper.findAllByOrderBySortNoAscIdAsc().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public BuildingDTO create(BuildingUpsertRequest request) {
        Building building = new Building();
        apply(request, building);
        building.touchForUpdate();
        buildingMapper.update(building);
        return toDto(building);
    }

    @Transactional
    public BuildingDTO update(Long id, BuildingUpsertRequest request) {
        Building building = buildingMapper.findById(id);
        if (building == null) {
            throw new BusinessException(40001, "楼栋不存在");
        }
        apply(request, building);
        building.touchForUpdate();
        buildingMapper.update(building);
        return toDto(building);
    }

    private void apply(BuildingUpsertRequest request, Building building) {
        building.setName(request.getName());
        building.setCampus(request.getCampus());
        building.setAddress(request.getAddress());
        building.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        building.setStatus(request.getStatus());
    }

    private BuildingDTO toDto(Building building) {
        BuildingDTO dto = new BuildingDTO();
        dto.setId(building.getId());
        dto.setName(building.getName());
        dto.setCampus(building.getCampus());
        dto.setAddress(building.getAddress());
        dto.setSortNo(building.getSortNo());
        dto.setStatus(building.getStatus());
        return dto;
    }
}
