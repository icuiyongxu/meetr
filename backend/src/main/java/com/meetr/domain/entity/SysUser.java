package com.meetr.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUser {

    private Long id;
    private String userId;
    private String name;
    /** BCrypt 加密后的密码，null 表示未设置密码（老用户兼容） */
    private String password;
    private String status = "ACTIVE";
    private Long createdAtMs;
    private Long updatedAtMs;
    private String email;
    private Boolean emailEnabled = false;
    /** 日历订阅 Token，用于生成 iCal 订阅 URL */
    private String calendarToken;

    public void initTimestampsForInsert() {
        long now = System.currentTimeMillis();
        if (createdAtMs == null) createdAtMs = now;
        if (updatedAtMs == null) updatedAtMs = now;
    }

    public void touchForUpdate() {
        updatedAtMs = System.currentTimeMillis();
    }
}
