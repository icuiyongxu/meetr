package com.meetr.config;

import com.meetr.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

@Configuration
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-Meetr-User-Id";

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            com.meetr.domain.UserContext.set("anonymous", com.meetr.domain.enums.UserRole.USER);
            return true;
        }

        boolean isAdmin = authService.hasRole(userId, "ADMIN");
        com.meetr.domain.UserContext.set(userId,
            isAdmin ? com.meetr.domain.enums.UserRole.ADMIN : com.meetr.domain.enums.UserRole.USER);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        com.meetr.domain.UserContext.clear();
    }
}
