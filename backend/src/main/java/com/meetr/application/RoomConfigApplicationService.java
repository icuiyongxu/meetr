package com.meetr.application;

import com.meetr.application.dto.RoomConfigDTO;
import com.meetr.application.dto.SaveRoomConfigRequest;
import com.meetr.domain.entity.MeetingRoom;
import com.meetr.domain.entity.RoomConfig;
import com.meetr.domain.enums.RoomStatus;
import com.meetr.domain.repository.MeetingRoomRepository;
import com.meetr.domain.repository.RoomConfigRepository;
import com.meetr.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class RoomConfigApplicationService {

    private final RoomConfigRepository roomConfigRepository;
    private final MeetingRoomRepository meetingRoomRepository;

    public RoomConfig getEffectiveConfigEntity(Long roomId) {
        if (roomId != null) {
            return roomConfigRepository.findFirstByRoomId(roomId)
                .orElseGet(this::getGlobalConfigEntity);
        }
        return getGlobalConfigEntity();
    }

    public RoomConfig getEnabledEffectiveConfigEntity(Long roomId) {
        RoomConfig config = getEffectiveConfigEntity(roomId);
        throwIfDisabled(config);
        return config;
    }

    public RoomConfigDTO getEffectiveConfig(Long roomId) {
        return toDto(getEffectiveConfigEntity(roomId));
    }

    @Transactional
    public RoomConfigDTO save(SaveRoomConfigRequest request) {
        validateRequest(request);
        if (request.getRoomId() != null && !meetingRoomRepository.existsById(request.getRoomId())) {
            throw new BusinessException(40001, "会议室不存在");
        }

        RoomConfig config = request.getRoomId() == null
            ? roomConfigRepository.findFirstByRoomIdIsNull().orElseGet(RoomConfig::new)
            : roomConfigRepository.findFirstByRoomId(request.getRoomId()).orElseGet(RoomConfig::new);

        config.setRoomId(request.getRoomId());
        config.setResolution(request.getResolution());
        config.setDefaultDuration(request.getDefaultDuration());
        config.setMorningStarts(request.getMorningStarts());
        config.setEveningEnds(request.getEveningEnds());
        config.setMinBookAheadMinutes(request.getMinBookAheadMinutes());
        config.setMaxBookAheadDays(request.getMaxBookAheadDays());
        config.setMinDurationMinutes(request.getMinDurationMinutes());
        config.setMaxDurationMinutes(request.getMaxDurationMinutes());
        config.setMaxPerDay(request.getMaxPerDay());
        config.setMaxPerWeek(request.getMaxPerWeek());
        config.setApprovalRequired(Boolean.TRUE.equals(request.getApprovalRequired()));
        config.setStatus(request.getStatus());
        return toDto(roomConfigRepository.save(config));
    }

    public RoomConfig getGlobalConfigEntity() {
        RoomConfig config = roomConfigRepository.findFirstByRoomIdIsNull()
            .orElseThrow(() -> new BusinessException(50001, "全局预约规则不存在"));
        return config;
    }

    private void validateRequest(SaveRoomConfigRequest request) {
        if (request.getResolution() == null || request.getResolution() <= 0) {
            throw new BusinessException(40002, "时间粒度必须大于0");
        }
        if (request.getMaxDurationMinutes() < request.getMinDurationMinutes()) {
            throw new BusinessException(40002, "最长预约时长不能小于最短预约时长");
        }
        if (request.getDefaultDuration() < request.getMinDurationMinutes()
            || request.getDefaultDuration() > request.getMaxDurationMinutes()) {
            throw new BusinessException(40002, "默认时长必须位于最短和最长预约时长之间");
        }
        LocalTime morning = LocalTime.parse(request.getMorningStarts());
        LocalTime evening = LocalTime.parse(request.getEveningEnds());
        if (!morning.isBefore(evening)) {
            throw new BusinessException(40002, "开放开始时间必须早于结束时间");
        }
    }

    private void throwIfDisabled(RoomConfig config) {
        if (config.getStatus() == RoomStatus.DISABLED) {
            throw new BusinessException(40003, "会议室预约规则已停用");
        }
    }

    private RoomConfigDTO toDto(RoomConfig config) {
        RoomConfigDTO dto = new RoomConfigDTO();
        dto.setId(config.getId());
        dto.setRoomId(config.getRoomId());
        dto.setResolution(config.getResolution());
        dto.setDefaultDuration(config.getDefaultDuration());
        dto.setMorningStarts(config.getMorningStarts());
        dto.setEveningEnds(config.getEveningEnds());
        dto.setMinBookAheadMinutes(config.getMinBookAheadMinutes());
        dto.setMaxBookAheadDays(config.getMaxBookAheadDays());
        dto.setMinDurationMinutes(config.getMinDurationMinutes());
        dto.setMaxDurationMinutes(config.getMaxDurationMinutes());
        dto.setMaxPerDay(config.getMaxPerDay());
        dto.setMaxPerWeek(config.getMaxPerWeek());
        dto.setApprovalRequired(config.getApprovalRequired());
        dto.setStatus(config.getStatus());
        return dto;
    }

}
