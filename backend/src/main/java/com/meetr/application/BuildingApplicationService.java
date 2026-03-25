package com.meetr.application;

import com.meetr.application.dto.BuildingDTO;
import com.meetr.application.dto.BuildingUpsertRequest;
import com.meetr.domain.entity.Building;
import com.meetr.domain.repository.BuildingRepository;
import com.meetr.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingApplicationService {

    private final BuildingRepository buildingRepository;

    public List<BuildingDTO> list() {
        return buildingRepository.findAllByOrderBySortNoAscIdAsc().stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public BuildingDTO create(BuildingUpsertRequest request) {
        Building building = new Building();
        apply(request, building);
        return toDto(buildingRepository.save(building));
    }

    @Transactional
    public BuildingDTO update(Long id, BuildingUpsertRequest request) {
        Building building = buildingRepository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "楼栋不存在"));
        apply(request, building);
        return toDto(buildingRepository.save(building));
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
