package com.meetr.application;

import com.meetr.domain.entity.SysRole;
import com.meetr.domain.entity.SysUser;
import com.meetr.domain.entity.SysUserRole;
import com.meetr.mapper.SysRoleMapper;
import com.meetr.mapper.SysUserMapper;
import com.meetr.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    public List<String> listAdminUserIds() {
        List<SysUser> users = sysUserMapper.findAll();
        return users.stream()
            .filter(user -> hasAdminRole(user.getId()))
            .map(SysUser::getUserId)
            .filter(userId -> userId != null && !userId.isBlank())
            .distinct()
            .toList();
    }

    private boolean hasAdminRole(Long userDbId) {
        List<SysUserRole> relations = sysUserRoleMapper.findByUserId(userDbId);
        Set<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        for (Long roleId : roleIds) {
            SysRole role = sysRoleMapper.findById(roleId);
            if (role != null && "ADMIN".equals(role.getCode())) {
                return true;
            }
        }
        return false;
    }
}
