package com.meetr.config;

import com.meetr.domain.entity.*;
import com.meetr.domain.repository.*;
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

    private final SysRoleRepository sysRoleRepository;
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysPermissionRepository sysPermissionRepository;
    private final SysRolePermissionRepository sysRolePermissionRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 权限定义 */
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

    /** 角色 → 权限映射 */
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

    private record PermissionDef(String code, String name) {}

    @Override
    public void run(String... args) {
        // 1. 创建权限
        for (PermissionDef pd : PERMISSIONS) {
            if (sysPermissionRepository.findByCode(pd.code).isEmpty()) {
                SysPermission p = SysPermission.builder()
                    .code(pd.code)
                    .name(pd.name)
                    .description(pd.name)
                    .build();
                sysPermissionRepository.save(p);
            }
        }

        // 2. 创建角色
        SysRole adminRole = sysRoleRepository.findByCode("ADMIN").orElseGet(() -> {
            SysRole r = new SysRole();
            r.setCode("ADMIN");
            r.setName("管理员");
            r.setDescription("系统管理员，拥有全部权限");
            return sysRoleRepository.save(r);
        });

        SysRole userRole = sysRoleRepository.findByCode("USER").orElseGet(() -> {
            SysRole r = new SysRole();
            r.setCode("USER");
            r.setName("普通用户");
            r.setDescription("普通用户，可预约会议室");
            return sysRoleRepository.save(r);
        });

        // 3. 绑定角色-权限
        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS.entrySet()) {
            String roleCode = entry.getKey();
            SysRole role = sysRoleRepository.findByCode(roleCode).orElse(null);
            if (role == null) continue;
            for (String permCode : entry.getValue()) {
                sysPermissionRepository.findByCode(permCode).ifPresent(perm -> {
                    if (sysRolePermissionRepository.findByRoleIdAndPermissionId(role.getId(), perm.getId()).isEmpty()) {
                        SysRolePermission rp = new SysRolePermission();
                        rp.setRole(role);
                        rp.setPermission(perm);
                        sysRolePermissionRepository.save(rp);
                    }
                });
            }
        }

        // 4. 创建初始管理员
        String adminUserId = System.getenv("MEETR_ADMIN_USER_ID");
        if (adminUserId == null || adminUserId.isBlank()) {
            adminUserId = "admin";
        }
        String adminPassword = System.getenv("MEETR_ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = "admin123";
        }

        if (!sysUserRepository.existsByUserId(adminUserId)) {
            SysUser adminUser = SysUser.builder()
                .userId(adminUserId)
                .name("管理员")
                .password(passwordEncoder.encode(adminPassword))
                .status("ACTIVE")
                .createdAtMs(System.currentTimeMillis())
                .build();
            adminUser = sysUserRepository.save(adminUser);

            SysUserRole ur = new SysUserRole();
            ur.setUser(adminUser);
            ur.setRole(adminRole);
            sysUserRoleRepository.save(ur);

            log.info("创建初始管理员: userId={}, password={}", adminUserId, adminPassword);
        }

        // 5. 给所有已存在但未分配角色的用户分配 USER 角色
        for (SysUser u : sysUserRepository.findAll()) {
            if (sysUserRoleRepository.findByUserId(u.getId()).isEmpty()) {
                SysUserRole ur = new SysUserRole();
                ur.setUser(u);
                ur.setRole(userRole);
                sysUserRoleRepository.save(ur);
            }
        }

        log.info("RBAC 初始化完成: 权限数={}, 角色数={}", PERMISSIONS.size(), ROLE_PERMISSIONS.size());
    }
}
