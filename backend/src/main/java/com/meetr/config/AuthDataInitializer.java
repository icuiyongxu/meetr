package com.meetr.config;

import com.meetr.domain.entity.SysPermission;
import com.meetr.domain.entity.SysRole;
import com.meetr.domain.entity.SysRolePermission;
import com.meetr.domain.entity.SysUser;
import com.meetr.domain.entity.SysUserRole;
import com.meetr.mapper.SysPermissionMapper;
import com.meetr.mapper.SysRoleMapper;
import com.meetr.mapper.SysRolePermissionMapper;
import com.meetr.mapper.SysUserMapper;
import com.meetr.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthDataInitializer implements CommandLineRunner {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final List<PermissionDef> PERMISSIONS = List.of(
        new PermissionDef("room:view", "查看会议室"),
        new PermissionDef("room:manage", "管理会议室"),
        new PermissionDef("building:view", "查看楼栋"),
        new PermissionDef("building:manage", "管理楼栋"),
        new PermissionDef("booking:view", "查看预约"),
        new PermissionDef("booking:manage", "管理预约"),
        new PermissionDef("booking:approve", "审批预约"),
        new PermissionDef("user:view", "查看用户"),
        new PermissionDef("user:manage", "管理用户"),
        new PermissionDef("role:manage", "管理角色"),
        new PermissionDef("config:view", "查看配置"),
        new PermissionDef("config:manage", "管理配置")
    );

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
        "ADMIN", List.of(
            "room:view", "room:manage",
            "building:view", "building:manage",
            "booking:view", "booking:manage", "booking:approve",
            "user:view", "user:manage",
            "role:manage",
            "config:view", "config:manage"
        ),
        "USER", List.of(
            "room:view",
            "building:view",
            "booking:view", "booking:manage"
        )
    );

    private record PermissionDef(String code, String name) {
    }

    @Override
    public void run(String... args) {
        for (PermissionDef pd : PERMISSIONS) {
            if (sysPermissionMapper.findByCode(pd.code) == null) {
                sysPermissionMapper.insert(SysPermission.builder()
                    .code(pd.code)
                    .name(pd.name)
                    .description(pd.name)
                    .build());
            }
        }

        SysRole adminRole = sysRoleMapper.findByCode("ADMIN");
        if (adminRole == null) {
            adminRole = new SysRole();
            adminRole.setCode("ADMIN");
            adminRole.setName("管理员");
            adminRole.setDescription("系统管理员，拥有全部权限");
            sysRoleMapper.insert(adminRole);
        }

        SysRole userRole = sysRoleMapper.findByCode("USER");
        if (userRole == null) {
            userRole = new SysRole();
            userRole.setCode("USER");
            userRole.setName("普通用户");
            userRole.setDescription("普通用户，可预约会议室");
            sysRoleMapper.insert(userRole);
        }

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS.entrySet()) {
            SysRole role = sysRoleMapper.findByCode(entry.getKey());
            if (role == null) {
                continue;
            }
            for (String permCode : entry.getValue()) {
                SysPermission perm = sysPermissionMapper.findByCode(permCode);
                if (perm != null && sysRolePermissionMapper.findByRoleIdAndPermissionId(role.getId(), perm.getId()) == null) {
                    SysRolePermission relation = new SysRolePermission();
                    relation.setRoleId(role.getId());
                    relation.setPermissionId(perm.getId());
                    sysRolePermissionMapper.insert(relation);
                }
            }
        }

        String adminUserId = System.getenv("MEETR_ADMIN_USER_ID");
        if (adminUserId == null || adminUserId.isBlank()) {
            adminUserId = "admin";
        }
        String adminPassword = System.getenv("MEETR_ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = "admin123";
        }

        if (!sysUserMapper.existsByUserId(adminUserId)) {
            SysUser adminUser = SysUser.builder()
                .userId(adminUserId)
                .name("管理员")
                .password(passwordEncoder.encode(adminPassword))
                .status("ACTIVE")
                .createdAtMs(System.currentTimeMillis())
                .build();
            sysUserMapper.insert(adminUser);

            SysUserRole relation = new SysUserRole();
            relation.setUserId(adminUser.getId());
            relation.setRoleId(adminRole.getId());
            sysUserRoleMapper.insert(relation);

            log.info("创建初始管理员: userId={}, password={}", adminUserId, adminPassword);
        }

        for (SysUser user : sysUserMapper.findAll()) {
            if (sysUserRoleMapper.findByUserId(user.getId()).isEmpty()) {
                SysUserRole relation = new SysUserRole();
                relation.setUserId(user.getId());
                relation.setRoleId(userRole.getId());
                sysUserRoleMapper.insert(relation);
            }
        }

        log.info("RBAC 初始化完成: 权限数={}, 角色数={}", PERMISSIONS.size(), ROLE_PERMISSIONS.size());
    }
}
