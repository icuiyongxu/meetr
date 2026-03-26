package com.meetr.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysRolePermission {

    private Long id;
    private Long roleId;
    private Long permissionId;
}
