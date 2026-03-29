package com.meetr.controller;

import com.meetr.config.RequirePermission;
import com.meetr.mapper.SysUserMapper;
import com.meetr.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper sysUserMapper;
    private final AuthService authService;

    public record UserDTO(Long id, String userId, String name, String status, List<String> roles, String email, Boolean emailEnabled) {}

    @RequirePermission("user:view")
    @GetMapping
    public ApiResponse<List<UserDTO>> list() {
        return ApiResponse.ok(sysUserMapper.findAll().stream().map(u -> {
            List<String> roles = authService.getUserRoles(u.getUserId());
            return new UserDTO(u.getId(), u.getUserId(), u.getName(), u.getStatus(), roles, u.getEmail(), u.getEmailEnabled());
        }).toList());
    }

    @RequirePermission("user:view")
    @GetMapping("/{userId}")
    public ApiResponse<UserDTO> get(@PathVariable String userId) {
        AuthService.UserDetail detail = authService.getUserDetail(userId);
        if (detail == null) {
            throw new com.meetr.exception.BusinessException(40401, "用户不存在");
        }
        return ApiResponse.ok(new UserDTO(detail.id(), detail.userId(), detail.name(), detail.status(), detail.roles(), detail.email(), detail.emailEnabled()));
    }

    /**
     * 获取当前用户的日历订阅 URL（完整地址）。
     */
    @RequirePermission("user:view")
    @GetMapping("/{userId}/calendar-url")
    public ApiResponse<String> getCalendarUrl(@PathVariable String userId, HttpServletRequest req) {
        String token = authService.getOrCreateCalendarToken(userId);
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
        return ApiResponse.ok(base + "/api/users/" + userId + "/ical?token=" + token);
    }

    /**
     * 重新生成日历订阅 Token（使旧链接失效）。
     */
    @RequirePermission("user:view")
    @PostMapping("/{userId}/calendar-regenerate")
    public ApiResponse<String> regenerateCalendarToken(@PathVariable String userId, HttpServletRequest req) {
        String token = authService.regenerateCalendarToken(userId);
        String base = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
        return ApiResponse.ok(base + "/api/users/" + userId + "/ical?token=" + token);
    }
}
