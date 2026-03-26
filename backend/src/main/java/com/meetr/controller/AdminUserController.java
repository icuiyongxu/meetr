package com.meetr.controller;

import com.meetr.config.RequirePermission;
import com.meetr.domain.entity.*;
import com.meetr.domain.repository.*;
import com.meetr.service.AuthService;
import lombok.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final AuthService authService;

    @RequirePermission("user:view")
    @GetMapping
    public ApiResponse<List<SysUserDTO>> list() {
        return ApiResponse.ok(sysUserRepository.findAll().stream().map(u -> {
            List<String> roles = authService.getUserRoles(u.getUserId());
            return new SysUserDTO(u.getId(), u.getUserId(), u.getName(), u.getStatus(), roles);
        }).toList());
    }

    @RequirePermission("user:manage")
    @PostMapping
    public ApiResponse<SysUserDTO> create(@RequestBody CreateUserRequest req) {
        if (sysUserRepository.existsByUserId(req.userId())) {
            throw new com.meetr.exception.BusinessException(40001, "用户已存在");
        }
        SysUser user = SysUser.builder()
            .userId(req.userId())
            .name(req.name() != null ? req.name() : req.userId())
            .status("ACTIVE")
            .createdAtMs(System.currentTimeMillis())
            .build();
        SysUser saved = sysUserRepository.save(user);

        // 分配 USER 角色
        sysRoleRepository.findByCode("USER").ifPresent(userRole -> {
            SysUserRole ur = new SysUserRole();
            ur.setUser(saved);
            ur.setRole(userRole);
            sysUserRoleRepository.save(ur);
        });

        return ApiResponse.ok(new SysUserDTO(saved.getId(), saved.getUserId(), saved.getName(), saved.getStatus(), List.of("USER")));
    }

    @RequirePermission("user:manage")
    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest req) {
        SysUser user = sysUserRepository.findById(id)
            .orElseThrow(() -> new com.meetr.exception.BusinessException(40001, "用户不存在"));

        // 删除旧角色
        sysUserRoleRepository.findByUserId(user.getId()).forEach(sysUserRoleRepository::delete);

        // 分配新角色
        for (String roleCode : req.roleCodes()) {
            sysRoleRepository.findByCode(roleCode).ifPresent(role -> {
                SysUserRole ur = new SysUserRole();
                ur.setUser(user);
                ur.setRole(role);
                sysUserRoleRepository.save(ur);
            });
        }
        return ApiResponse.ok(null);
    }

    public record SysUserDTO(Long id, String userId, String name, String status, List<String> roles) {}
    public record CreateUserRequest(String userId, String name) {}
    public record AssignRolesRequest(List<String> roleCodes) {}
}
