package com.meetr.controller;

import com.meetr.config.RequirePermission;
import com.meetr.domain.entity.*;
import com.meetr.mapper.SysPermissionMapper;
import com.meetr.mapper.SysRoleMapper;
import com.meetr.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    // ---------- 角色列表 ----------
    @RequirePermission("role:manage")
    @GetMapping
    public ApiResponse<List<SysRole>> listRoles() {
        return ApiResponse.ok(sysRoleMapper.findAll());
    }

    @RequirePermission("role:manage")
    @PostMapping
    public ApiResponse<SysRole> createRole(@RequestBody CreateRoleRequest req) {
        if (sysRoleMapper.findByCode(req.code().toUpperCase()) != null) {
            throw new com.meetr.exception.BusinessException(40001, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(req.code().toUpperCase());
        role.setName(req.name());
        role.setDescription(req.description());
        sysRoleMapper.insert(role);
        return ApiResponse.ok(role);
    }

    public record CreateRoleRequest(String code, String name, String description) {}

    // ---------- 权限列表 ----------
    @RequirePermission("role:manage")
    @GetMapping("/permissions")
    public ApiResponse<List<SysPermission>> listPermissions() {
        return ApiResponse.ok(sysPermissionMapper.findAll());
    }

    // ---------- 获取角色拥有的权限 ----------
    @RequirePermission("role:manage")
    @GetMapping("/{roleId}/permissions")
    public ApiResponse<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        List<Long> permIds = sysRolePermissionMapper.findByRoleId(roleId).stream()
            .map(com.meetr.domain.entity.SysRolePermission::getPermissionId)
            .toList();
        return ApiResponse.ok(permIds);
    }

    // ---------- 分配角色权限 ----------
    @RequirePermission("role:manage")
    @PutMapping("/{roleId}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable Long roleId, @RequestBody AssignPermissionsRequest req) {
        SysRole role = sysRoleMapper.findById(roleId);
        if (role == null) {
            throw new com.meetr.exception.BusinessException(40001, "角色不存在");
        }

        // 删除旧权限
        sysRolePermissionMapper.deleteByRoleId(roleId);

        // 绑定新权限
        for (Long permId : req.permissionIds()) {
            if (sysPermissionMapper.findById(permId) != null) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }
        return ApiResponse.ok(null);
    }

    public record AssignPermissionsRequest(List<Long> permissionIds) {}
}
