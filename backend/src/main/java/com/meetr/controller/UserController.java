package com.meetr.controller;

import com.meetr.domain.entity.SysUser;
import com.meetr.domain.repository.SysUserRepository;
import com.meetr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserRepository sysUserRepository;
    private final AuthService authService;

    public record UserDTO(Long id, String userId, String name, String status, List<String> roles) {}

    @GetMapping
    public ApiResponse<List<UserDTO>> list() {
        return ApiResponse.ok(sysUserRepository.findAll().stream().map(u -> {
            List<String> roles = authService.getUserRoles(u.getUserId());
            return new UserDTO(u.getId(), u.getUserId(), u.getName(), u.getStatus(), roles);
        }).toList());
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserDTO> get(@PathVariable String userId) {
        AuthService.UserDetail detail = authService.getUserDetail(userId);
        if (detail == null) {
            throw new com.meetr.exception.BusinessException(40401, "用户不存在");
        }
        return ApiResponse.ok(new UserDTO(detail.id(), detail.userId(), detail.name(), detail.status(), detail.roles()));
    }
}
