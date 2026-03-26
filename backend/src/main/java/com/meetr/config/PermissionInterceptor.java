package com.meetr.config;

import com.meetr.domain.UserContext;
import com.meetr.exception.BusinessException;
import com.meetr.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限校验拦截器。
 * 读取方法上的 @RequirePermission 注解，若无则放行，有则调用 AuthService 校验。
 * 运行在 AdminInterceptor 之后，此时 UserContext 已填充。
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        RequirePermission ann = hm.getMethodAnnotation(RequirePermission.class);
        if (ann == null) {
            // 方法未标注权限要求，放行
            return true;
        }

        String userId = UserContext.currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(40101, "未登录或登录已过期");
        }

        String requiredPerm = ann.value();
        if (!authService.hasPermission(userId, requiredPerm)) {
            throw new BusinessException(40302, "没有权限：" + requiredPerm);
        }

        return true;
    }
}
