package com.meetr.service;

import com.meetr.domain.entity.*;
import com.meetr.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRolePermissionRepository sysRolePermissionRepository;

    /**
     * userId 登录（userId 存在则返回，不存在则自动注册为 USER）。
     */
    @Transactional
    public SysUser loginOrRegister(String userId) {
        return sysUserRepository.findByUserId(userId).orElseGet(() -> {
            SysUser user = SysUser.builder()
                .userId(userId)
                .name(userId)
                .status("ACTIVE")
                .createdAtMs(System.currentTimeMillis())
                .build();
            SysUser saved = sysUserRepository.save(user);

            // 自动分配 USER 角色
            sysRoleRepository.findByCode("USER").ifPresent(userRole -> {
                SysUserRole ur = new SysUserRole();
                ur.setUser(saved);
                ur.setRole(userRole);
                sysUserRoleRepository.save(ur);
            });
            return saved;
        });
    }

    /**
     * 获取用户的角色列表（role code）。
     */
    public List<String> getUserRoles(String userId) {
        return sysUserRepository.findByUserId(userId)
            .map(user -> sysUserRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getCode())
                .toList())
            .orElse(List.of());
    }

    /**
     * 判断用户是否拥有某角色。
     */
    public boolean hasRole(String userId, String roleCode) {
        return getUserRoles(userId).contains(roleCode);
    }

    /**
     * 判断用户是否拥有某权限。
     */
    public boolean hasPermission(String userId, String permissionCode) {
        return getUserRoles(userId).stream()
            .filter(code -> "ADMIN".equals(code)) // ADMIN 拥有所有权限
            .findAny()
            .isPresent()
            || sysUserRepository.findByUserId(userId)
                .map(user -> sysUserRoleRepository.findByUserId(user.getId()).stream()
                    .anyMatch(ur -> sysRolePermissionRepository.findByRoleId(ur.getRole().getId()).stream()
                        .anyMatch(rp -> rp.getPermission().getCode().equals(permissionCode))))
                .orElse(false);
    }

    /**
     * 获取用户详情（含角色列表）。
     */
    public UserDetail getUserDetail(String userId) {
        SysUser user = sysUserRepository.findByUserId(userId).orElse(null);
        if (user == null) return null;
        List<String> roles = getUserRoles(userId);
        return new UserDetail(user.getId(), user.getUserId(), user.getName(), user.getStatus(), roles);
    }

    public record UserDetail(Long id, String userId, String name, String status, List<String> roles) {}
}
