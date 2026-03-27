package com.meetr.controller;

import com.meetr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * userId + 密码登录。
     * 无密码时（老用户）走 userId 直接登录。
     */
    @PostMapping("/login")
    public ApiResponse<AuthService.UserDetail> login(@RequestBody LoginRequest req) {
        AuthService.UserDetail detail;
        if (req.password() != null && !req.password().isBlank()) {
            // 有密码，走密码验证
            authService.login(req.userId(), req.password());
            detail = authService.getUserDetail(req.userId());
        } else {
            // 无密码，老用户或测试场景
            authService.loginOrRegister(req.userId());
            detail = authService.getUserDetail(req.userId());
        }
        return ApiResponse.ok(detail);
    }

    /**
     * 注册新用户。
     */
    @PostMapping("/register")
    public ApiResponse<AuthService.UserDetail> register(@RequestBody RegisterRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new com.meetr.exception.BusinessException(40001, "密码不能为空");
        }
        authService.register(req.userId(), req.name(), req.password());
        return ApiResponse.ok(authService.getUserDetail(req.userId()));
    }

    /**
     * 获取当前用户信息。
     */
    @GetMapping("/me")
    public ApiResponse<AuthService.UserDetail> me(@RequestParam String userId) {
        return ApiResponse.ok(authService.getUserDetail(userId));
    }

    /**
     * 更新个人资料（昵称、密码、邮箱、邮件通知开关）。
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody UpdateProfileRequest req) {
        authService.updateProfile(req.userId(), req.name(), req.password(), req.email(), req.emailEnabled());
        return ApiResponse.ok(null);
    }

    public record LoginRequest(String userId, String password) {}
    public record RegisterRequest(String userId, String name, String password) {}
    public record UpdateProfileRequest(String userId, String name, String password, String email, Boolean emailEnabled) {}
}
