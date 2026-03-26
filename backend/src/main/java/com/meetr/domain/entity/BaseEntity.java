package com.meetr.domain.entity;

import lombok.Data;

@Data
public abstract class BaseEntity {

    /** 存 UTC 毫秒 */
    private Long createdAtMs;

    /** 存 UTC 毫秒 */
    private Long updatedAtMs;

    public void initTimestampsForInsert() {
        long now = System.currentTimeMillis();
        if (createdAtMs == null) {
            createdAtMs = now;
        }
        if (updatedAtMs == null) {
            updatedAtMs = now;
        }
    }

    public void touchForUpdate() {
        updatedAtMs = System.currentTimeMillis();
    }
}
