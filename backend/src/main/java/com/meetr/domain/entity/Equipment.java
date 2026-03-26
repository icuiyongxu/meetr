package com.meetr.domain.entity;

import lombok.Data;

@Data
public class Equipment {
    private Long id;
    private String code;    // 唯一编码，如 projector
    private String name;    // 显示名称，如 投影仪
    private String status;  // ACTIVE / DISABLED
}
