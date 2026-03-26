package com.meetr.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "sys_user_role")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private SysUser user;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private SysRole role;
}
