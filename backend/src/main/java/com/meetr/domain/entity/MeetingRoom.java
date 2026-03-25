package com.meetr.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetr.domain.enums.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "meeting_room")
@EqualsAndHashCode(callSuper = true)
public class MeetingRoom extends BaseEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long buildingId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String floor;

    @Column(nullable = false)
    private Integer capacity = 0;

    @Column(columnDefinition = "json")
    private String equipment = "[]";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.ENABLED;

    @Column(length = 500)
    private String remark;

    @Transient
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
