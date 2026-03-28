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
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthDataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public void run(String... args) {
        SysRole adminRole = ensureRole("ADMIN", "管理员", "系统管理员，拥有所有权限");
        SysRole userRole = ensureRole("USER", "普通用户", "普通业务用户");

        Map<String, String> permissions = new LinkedHashMap<>();
        permissions.put("building:view", "查看楼栋");
        permissions.put("building:manage", "管理楼栋");
        permissions.put("room:view", "查看会议室");
        permissions.put("room:manage", "管理会议室");
        permissions.put("booking:view", "查看预约");
        permissions.put("booking:manage", "管理预约");
        permissions.put("booking:approve", "审批预约");
        permissions.put("config:view", "查看规则配置");
        permissions.put("config:manage", "管理规则配置");
        permissions.put("user:view", "查看用户");
        permissions.put("user:manage", "管理用户");
        permissions.put("role:manage", "管理角色与权限");
        permissions.put("notification:view", "查看通知");
        permissions.put("notification:manage", "管理通知");

        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            ensurePermission(entry.getKey(), entry.getValue(), entry.getValue());
        }

        grantAllPermissions(adminRole);
        grantUserDefaultPermissions(userRole);
        ensureUsersHaveBaseRole(userRole.getId());

        log.info("AuthDataInitializer 完成：roles={}, permissions={}",
            sysRoleMapper.findAll().size(), sysPermissionMapper.findAll().size());
    }

    private SysRole ensureRole(String code, String name, String description) {
        SysRole role = sysRoleMapper.findByCode(code);
        if (role != null) {
            return role;
        }
        role = new SysRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        sysRoleMapper.insert(role);
        log.info("初始化角色：{}", code);
        return role;
    }

    private SysPermission ensurePermission(String code, String name, String description) {
        SysPermission permission = sysPermissionMapper.findByCode(code);
        if (permission != null) {
            return permission;
        }
        permission = SysPermission.builder()
            .code(code)
            .name(name)
            .description(description)
            .build();
        sysPermissionMapper.insert(permission);
        log.info("初始化权限：{}", code);
        return permission;
    }

    private void grantAllPermissions(SysRole adminRole) {
        List<SysPermission> allPermissions = sysPermissionMapper.findAll();
        for (SysPermission permission : allPermissions) {
            bindRolePermissionIfAbsent(adminRole.getId(), permission.getId());
        }
    }

    private void grantUserDefaultPermissions(SysRole userRole) {
        List<String> codes = List.of(
            "building:view",
            "room:view",
            "booking:view",
            "booking:manage",
            "notification:view",
            "notification:manage"
        );
        for (String code : codes) {
            SysPermission permission = sysPermissionMapper.findByCode(code);
            if (permission != null) {
                bindRolePermissionIfAbsent(userRole.getId(), permission.getId());
            }
        }
    }

    private void bindRolePermissionIfAbsent(Long roleId, Long permissionId) {
        if (sysRolePermissionMapper.findByRoleIdAndPermissionId(roleId, permissionId) != null) {
            return;
        }
        SysRolePermission relation = new SysRolePermission();
        relation.setRoleId(roleId);
        relation.setPermissionId(permissionId);
        sysRolePermissionMapper.insert(relation);
    }

    private void ensureUsersHaveBaseRole(Long userRoleId) {
        List<SysUser> allUsers = sysUserMapper.findAll();
        for (SysUser user : allUsers) {
            if (sysUserRoleMapper.findByUserId(user.getId()).isEmpty()) {
                SysUserRole relation = new SysUserRole();
                relation.setUserId(user.getId());
                relation.setRoleId(userRoleId);
                sysUserRoleMapper.insert(relation);
                log.info("为未分配角色用户 {} 补发 USER 角色", user.getUserId());
            }
        }
    }
}
