package com.meetr.controller;

import com.meetr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * userId 登录，自动注册。
     * Headers: X-Meetr-User-Id: user_xxx
     */
    @PostMapping("/login")
    public ApiResponse<AuthService.UserDetail> login(@RequestParam String userId) {
        authService.loginOrRegister(userId);
        return ApiResponse.ok(authService.getUserDetail(userId));
    }

    /**
     * 获取当前用户信息。
     */
    @GetMapping("/me")
    public ApiResponse<AuthService.UserDetail> me(@RequestParam String userId) {
        return ApiResponse.ok(authService.getUserDetail(userId));
    }
}
