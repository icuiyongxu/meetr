package com.meetr.service;

import com.meetr.domain.entity.*;
import com.meetr.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---------- 登录（userId + 密码）----------

    /**
     * 用户登录，验证密码。
     * 首次登录（用户不存在）走注册流程。
     */
    @Transactional
    public SysUser login(String userId, String password) {
        SysUser user = sysUserRepository.findByUserId(userId).orElse(null);
        if (user == null) {
            // 首次登录，自动注册（不设密码，后续需设置）
            return loginOrRegister(userId);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new com.meetr.exception.BusinessException(40102, "账号已被停用");
        }
        if (user.getPassword() != null && !passwordEncoder.matches(password, user.getPassword())) {
            throw new com.meetr.exception.BusinessException(40101, "密码错误");
        }
        return user;
    }

    /**
     * 自动注册（无密码，仅 userId）。
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
            assignRole(saved, "USER");
            return saved;
        });
    }

    /**
     * 注册新用户（管理员创建或用户自助注册）。
     */
    @Transactional
    public SysUser register(String userId, String name, String password) {
        if (sysUserRepository.findByUserId(userId).isPresent()) {
            throw new com.meetr.exception.BusinessException(40001, "用户已存在");
        }
        SysUser user = SysUser.builder()
            .userId(userId)
            .name(name != null ? name : userId)
            .password(passwordEncoder.encode(password))
            .status("ACTIVE")
            .createdAtMs(System.currentTimeMillis())
            .build();
        SysUser saved = sysUserRepository.save(user);
        assignRole(saved, "USER");
        return saved;
    }

    /**
     * 修改用户信息（管理员）。
     */
    @Transactional
    public SysUser updateUser(Long id, String name, String password, String status) {
        SysUser user = sysUserRepository.findById(id)
            .orElseThrow(() -> new com.meetr.exception.BusinessException(40401, "用户不存在"));
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (status != null) {
            user.setStatus(status);
        }
        return sysUserRepository.save(user);
    }

    /**
     * 删除用户。
     */
    @Transactional
    public void deleteUser(Long id) {
        SysUser user = sysUserRepository.findById(id)
            .orElseThrow(() -> new com.meetr.exception.BusinessException(40401, "用户不存在"));
        sysUserRoleRepository.findByUserId(user.getId()).forEach(sysUserRoleRepository::delete);
        sysUserRepository.delete(user);
    }

    /**
     * 启用/停用用户。
     */
    @Transactional
    public SysUser setUserStatus(Long id, String status) {
        SysUser user = sysUserRepository.findById(id)
            .orElseThrow(() -> new com.meetr.exception.BusinessException(40401, "用户不存在"));
        user.setStatus(status);
        return sysUserRepository.save(user);
    }

    /**
     * 分配角色。
     */
    @Transactional
    public void assignRoles(Long id, List<String> roleCodes) {
        SysUser user = sysUserRepository.findById(id)
            .orElseThrow(() -> new com.meetr.exception.BusinessException(40401, "用户不存在"));
        sysUserRoleRepository.findByUserId(user.getId()).forEach(sysUserRoleRepository::delete);
        for (String code : roleCodes) {
            sysRoleRepository.findByCode(code).ifPresent(role -> {
                SysUserRole ur = new SysUserRole();
                ur.setUser(user);
                ur.setRole(role);
                sysUserRoleRepository.save(ur);
            });
        }
    }

    private void assignRole(SysUser user, String roleCode) {
        sysRoleRepository.findByCode(roleCode).ifPresent(role -> {
            SysUserRole ur = new SysUserRole();
            ur.setUser(user);
            ur.setRole(role);
            sysUserRoleRepository.save(ur);
        });
    }

    // ---------- 原有方法 ----------

    public List<String> getUserRoles(String userId) {
        return sysUserRepository.findByUserId(userId)
            .map(user -> sysUserRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().getCode())
                .toList())
            .orElse(List.of());
    }

    public List<SysUser> getAllUsers() {
        return sysUserRepository.findAll();
    }

    public boolean hasRole(String userId, String roleCode) {
        return getUserRoles(userId).contains(roleCode);
    }

    public boolean hasPermission(String userId, String permissionCode) {
        return getUserRoles(userId).stream()
            .filter(code -> "ADMIN".equals(code))
            .findAny()
            .isPresent()
            || sysUserRepository.findByUserId(userId)
                .map(user -> sysUserRoleRepository.findByUserId(user.getId()).stream()
                    .anyMatch(ur -> sysRolePermissionRepository.findByRoleId(ur.getRole().getId()).stream()
                        .anyMatch(rp -> rp.getPermission().getCode().equals(permissionCode))))
                .orElse(false);
    }

    public UserDetail getUserDetail(String userId) {
        SysUser user = sysUserRepository.findByUserId(userId).orElse(null);
        if (user == null) return null;
        List<String> roles = getUserRoles(userId);
        return new UserDetail(user.getId(), user.getUserId(), user.getName(), user.getStatus(), roles);
    }

    public record UserDetail(Long id, String userId, String name, String status, List<String> roles) {}
}
