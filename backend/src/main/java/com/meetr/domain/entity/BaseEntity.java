package com.meetr.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.ZoneId;

@Data
@MappedSuperclass
public abstract class BaseEntity {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    /** 存北京时间的 UTC 毫秒 */
    @Column(nullable = false, updatable = false)
    private Long createdAtMs;

    /** 存北京时间的 UTC 毫秒 */
    @Column(nullable = false)
    private Long updatedAtMs;

    @PrePersist
    public void prePersist() {
        long now = System.currentTimeMillis();
        if (createdAtMs == null) {
            createdAtMs = now;
        }
        updatedAtMs = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAtMs = System.currentTimeMillis();
    }
}
