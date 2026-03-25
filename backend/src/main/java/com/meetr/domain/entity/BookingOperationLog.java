package com.meetr.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "booking_operation_log")
@EqualsAndHashCode(callSuper = true)
public class BookingOperationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false, length = 50)
    private String operationType;

    @Column(length = 64)
    private String operatorId;

    @Column(length = 100)
    private String operatorName;

    @Column(nullable = false, length = 1000)
    private String content;
}
