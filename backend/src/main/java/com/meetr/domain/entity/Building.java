package com.meetr.domain.entity;

import com.meetr.domain.enums.BuildingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "building")
@EqualsAndHashCode(callSuper = true)
public class Building extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String campus;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private Integer sortNo = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BuildingStatus status = BuildingStatus.ACTIVE;
}
