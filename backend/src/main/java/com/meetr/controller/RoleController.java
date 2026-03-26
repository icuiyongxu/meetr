package com.meetr.controller;

import com.meetr.config.RequirePermission;
import com.meetr.domain.entity.*;
import com.meetr.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleRepository sysRoleRepository;
    private final SysPermissionRepository sysPermissionRepository;
    private final SysRolePermissionRepository sysRolePermissionRepository;

    // ---------- 角色列表 ----------
    @RequirePermission("role:manage")
    @GetMapping
    public ApiResponse<List<SysRole>> listRoles() {
        return ApiResponse.ok(sysRoleRepository.findAll());
    }

    @RequirePermission("role:manage")
    @PostMapping
    public ApiResponse<SysRole> createRole(@RequestBody CreateRoleRequest req) {
        if (sysRoleRepository.findByCode(req.code().toUpperCase()).isPresent()) {
            throw new com.meetr.exception.BusinessException(40001, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(req.code().toUpperCase());
        role.setName(req.name());
        role.setDescription(req.description());
        return ApiResponse.ok(sysRoleRepository.save(role));
    }

    public record CreateRoleRequest(String code, String name, String description) {}

    // ---------- 权限列表 ----------
    @RequirePermission("role:manage")
    @GetMapping("/permissions")
    public ApiResponse<List<SysPermission>> listPermissions() {
        return ApiResponse.ok(sysPermissionRepository.findAll());
    }

    // ---------- 获取角色拥有的权限 ----------
    @RequirePermission("role:manage")
    @GetMapping("/{roleId}/permissions")
    public ApiResponse<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        List<Long> permIds = sysRolePermissionRepository.findByRoleId(roleId).stream()
            .map(rp -> rp.getPermission().getId())
            .toList();
        return ApiResponse.ok(permIds);
    }

    // ---------- 分配角色权限 ----------
    @RequirePermission("role:manage")
    @PutMapping("/{roleId}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable Long roleId, @RequestBody AssignPermissionsRequest req) {
        SysRole role = sysRoleRepository.findById(roleId)
            .orElseThrow(() -> new com.meetr.exception.BusinessException(40001, "角色不存在"));

        // 删除旧权限
        sysRolePermissionRepository.deleteByRoleId(roleId);

        // 绑定新权限
        for (Long permId : req.permissionIds()) {
            sysPermissionRepository.findById(permId).ifPresent(perm -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setRole(role);
                rp.setPermission(perm);
                sysRolePermissionRepository.save(rp);
            });
        }
        return ApiResponse.ok(null);
    }

    public record AssignPermissionsRequest(List<Long> permissionIds) {}
}
