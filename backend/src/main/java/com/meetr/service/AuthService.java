package com.meetr.service;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public SysUser login(String userId, String password) {
        SysUser user = sysUserMapper.findByUserId(userId);
        if (user == null) {
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

    @Transactional
    public SysUser loginOrRegister(String userId) {
        SysUser existing = sysUserMapper.findByUserId(userId);
        if (existing != null) {
            return existing;
        }
        SysUser user = SysUser.builder()
            .userId(userId)
            .name(userId)
            .status("ACTIVE")
            .calendarToken(UUID.randomUUID().toString().replace("-", ""))
            .build();
        user.initTimestampsForInsert();
        sysUserMapper.insert(user);
        assignRole(user, "USER");
        return user;
    }

    @Transactional
    public SysUser register(String userId, String name, String password) {
        if (sysUserMapper.findByUserId(userId) != null) {
            throw new com.meetr.exception.BusinessException(40001, "用户已存在");
        }
        SysUser user = SysUser.builder()
            .userId(userId)
            .name(name != null ? name : userId)
            .password(passwordEncoder.encode(password))
            .status("ACTIVE")
            .calendarToken(UUID.randomUUID().toString().replace("-", ""))
            .build();
        user.initTimestampsForInsert();
        sysUserMapper.insert(user);
        assignRole(user, "USER");
        return user;
    }

    @Transactional
    public SysUser updateUser(Long id, String name, String password, String status, String email, Boolean emailEnabled) {
        SysUser user = requireUser(id);
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (status != null) {
            user.setStatus(status);
        }
        if (email != null) {
            user.setEmail(email.isBlank() ? null : email);
        }
        if (emailEnabled != null) {
            user.setEmailEnabled(emailEnabled);
        }
        user.touchForUpdate();
        sysUserMapper.update(user);
        return user;
    }

    @Transactional
    public void deleteUser(Long id) {
        SysUser user = requireUser(id);
        sysUserRoleMapper.deleteByUserId(user.getId());
        sysUserMapper.deleteById(user.getId());
    }

    @Transactional
    public SysUser setUserStatus(Long id, String status) {
        SysUser user = requireUser(id);
        user.setStatus(status);
        user.touchForUpdate();
        sysUserMapper.update(user);
        return user;
    }

    @Transactional
    public void assignRoles(Long id, List<String> roleCodes) {
        SysUser user = requireUser(id);
        sysUserRoleMapper.deleteByUserId(user.getId());
        for (String code : roleCodes) {
            SysRole role = sysRoleMapper.findByCode(code);
            if (role != null) {
                SysUserRole relation = new SysUserRole();
                relation.setUserId(user.getId());
                relation.setRoleId(role.getId());
                sysUserRoleMapper.insert(relation);
            }
        }
    }

    private void assignRole(SysUser user, String roleCode) {
        SysRole role = sysRoleMapper.findByCode(roleCode);
        if (role != null) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(user.getId());
            relation.setRoleId(role.getId());
            sysUserRoleMapper.insert(relation);
        }
    }

    public List<String> getUserRoles(String userId) {
        SysUser user = sysUserMapper.findByUserId(userId);
        if (user == null) {
            return List.of();
        }
        return sysUserRoleMapper.findByUserId(user.getId()).stream()
            .map(relation -> sysRoleMapper.findById(relation.getRoleId()))
            .filter(Objects::nonNull)
            .map(SysRole::getCode)
            .toList();
    }

    public List<SysUser> getAllUsers() {
        return sysUserMapper.findAll();
    }

    public boolean hasRole(String userId, String roleCode) {
        return getUserRoles(userId).contains(roleCode);
    }

    public boolean hasPermission(String userId, String permissionCode) {
        return getUserRoles(userId).stream().anyMatch("ADMIN"::equals) || hasPermissionInternal(userId, permissionCode);
    }

    public UserDetail getUserDetail(String userId) {
        SysUser user = sysUserMapper.findByUserIdWithEmail(userId);
        if (user == null) {
            return null;
        }
        return new UserDetail(user.getId(), user.getUserId(), user.getName(), user.getStatus(),
            getUserRoles(userId), getUserPermissions(userId), user.getEmail(), user.getEmailEnabled(),
            getOrCreateCalendarToken(userId));
    }

    public List<String> getUserPermissions(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        if (hasRole(userId, "ADMIN")) {
            return sysPermissionMapper.findAll().stream()
                .map(SysPermission::getCode)
                .toList();
        }
        java.util.Set<String> perms = new java.util.HashSet<>();
        SysUser user = sysUserMapper.findByUserId(userId);
        if (user == null) {
            return List.of();
        }
        for (SysUserRole relation : sysUserRoleMapper.findByUserId(user.getId())) {
            for (SysRolePermission rolePermission : sysRolePermissionMapper.findByRoleId(relation.getRoleId())) {
                SysPermission permission = sysPermissionMapper.findById(rolePermission.getPermissionId());
                if (permission != null) {
                    perms.add(permission.getCode());
                }
            }
        }
        return perms.stream().sorted().toList();
    }

    @Transactional
    public void updateProfile(String userId, String name, String password, String email, Boolean emailEnabled) {
        SysUser user = sysUserMapper.findByUserIdWithEmail(userId);
        if (user == null) {
            throw new com.meetr.exception.BusinessException(40401, "用户不存在");
        }
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (email != null) {
            user.setEmail(email.isBlank() ? null : email);
        }
        if (emailEnabled != null) {
            user.setEmailEnabled(emailEnabled);
        }
        user.touchForUpdate();
        sysUserMapper.update(user);
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            throw new com.meetr.exception.BusinessException(40401, "用户不存在");
        }
        return user;
    }

    private boolean hasPermissionInternal(String userId, String permissionCode) {
        SysUser user = sysUserMapper.findByUserId(userId);
        if (user == null) {
            return false;
        }
        for (SysUserRole relation : sysUserRoleMapper.findByUserId(user.getId())) {
            for (SysRolePermission rolePermission : sysRolePermissionMapper.findByRoleId(relation.getRoleId())) {
                SysPermission permission = sysPermissionMapper.findById(rolePermission.getPermissionId());
                if (permission != null && permissionCode.equals(permission.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    public record UserDetail(Long id, String userId, String name, String status, List<String> roles,
                             List<String> permissions, String email, Boolean emailEnabled, String calendarToken) {
    }

    /**
     * 获取用户的日历订阅 Token（如果没有则自动创建）。
     */
    public String getOrCreateCalendarToken(String userId) {
        SysUser user = sysUserMapper.findByUserId(userId);
        if (user == null) {
            throw new com.meetr.exception.BusinessException(40401, "用户不存在");
        }
        if (user.getCalendarToken() == null || user.getCalendarToken().isBlank()) {
            user.setCalendarToken(UUID.randomUUID().toString().replace("-", ""));
            sysUserMapper.update(user);
        }
        return user.getCalendarToken();
    }

    /**
     * 重新生成日历订阅 Token（旧的立即失效）。
     */
    public String regenerateCalendarToken(String userId) {
        SysUser user = sysUserMapper.findByUserId(userId);
        if (user == null) {
            throw new com.meetr.exception.BusinessException(40401, "用户不存在");
        }
        String newToken = UUID.randomUUID().toString().replace("-", "");
        user.setCalendarToken(newToken);
        user.touchForUpdate();
        sysUserMapper.update(user);
        return newToken;
    }
}
