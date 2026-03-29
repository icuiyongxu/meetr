package com.meetr.controller;

import com.meetr.config.RequirePermission;
import com.meetr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    public record UserDTO(Long id, String userId, String name, String status, List<String> roles, String email, Boolean emailEnabled) {}
    public record CreateUserRequest(String userId, String name, String password) {}
    public record UpdateUserRequest(String name, String password, String status, String email, Boolean emailEnabled) {}

    @RequirePermission("user:view")
    @GetMapping
    public ApiResponse<List<UserDTO>> list() {
        return ApiResponse.ok(authService.getAllUsers().stream()
            .map(u -> new UserDTO(u.getId(), u.getUserId(), u.getName(), u.getStatus(), authService.getUserRoles(u.getUserId()), u.getEmail(), u.getEmailEnabled()))
            .toList());
    }

    @RequirePermission("user:manage")
    @PostMapping
    public ApiResponse<UserDTO> create(@RequestBody CreateUserRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new com.meetr.exception.BusinessException(40001, "密码不能为空");
        }
        var user = authService.register(req.userId(), req.name(), req.password());
        return ApiResponse.ok(new UserDTO(user.getId(), user.getUserId(), user.getName(), user.getStatus(), authService.getUserRoles(user.getUserId()), user.getEmail(), user.getEmailEnabled()));
    }

    @RequirePermission("user:manage")
    @PutMapping("/{id}")
    public ApiResponse<UserDTO> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        var user = authService.updateUser(id, req.name(), req.password(), req.status(), req.email(), req.emailEnabled());
        return ApiResponse.ok(new UserDTO(user.getId(), user.getUserId(), user.getName(), user.getStatus(), authService.getUserRoles(user.getUserId()), user.getEmail(), user.getEmailEnabled()));
    }

    @RequirePermission("user:manage")
    @PutMapping("/{id}/status")
    public ApiResponse<UserDTO> setStatus(@PathVariable Long id, @RequestBody SetStatusRequest req) {
        var user = authService.setUserStatus(id, req.status());
        return ApiResponse.ok(new UserDTO(user.getId(), user.getUserId(), user.getName(), user.getStatus(), authService.getUserRoles(user.getUserId()), user.getEmail(), user.getEmailEnabled()));
    }

    @RequirePermission("user:manage")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        authService.deleteUser(id);
        return ApiResponse.ok(null);
    }

    @RequirePermission("user:manage")
    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest req) {
        authService.assignRoles(id, req.roleCodes());
        return ApiResponse.ok(null);
    }

    public record SetStatusRequest(String status) {}
    public record AssignRolesRequest(List<String> roleCodes) {}
}
