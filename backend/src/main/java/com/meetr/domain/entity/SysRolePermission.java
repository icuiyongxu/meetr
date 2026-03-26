package com.meetr.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "sys_role_permission")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private SysRole role;

    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private SysPermission permission;
}
