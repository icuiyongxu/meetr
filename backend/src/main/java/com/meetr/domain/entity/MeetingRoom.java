package com.meetr.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetr.domain.enums.RoomStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingRoom extends BaseEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long id;
    private Long buildingId;
    private String name;
    private String floor;
    private Integer capacity = 0;
    private String equipment = "[]";
    private RoomStatus status = RoomStatus.ENABLED;
    private String remark;

    public List<String> equipmentItems() {
        if (equipment == null || equipment.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(equipment, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse meeting room equipment", ex);
        }
    }

    public void setEquipmentItems(List<String> items) {
        try {
            this.equipment = OBJECT_MAPPER.writeValueAsString(items == null ? List.of() : items);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize meeting room equipment", ex);
        }
    }
}
