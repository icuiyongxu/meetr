package com.meetr.domain;

import com.meetr.domain.enums.UserRole;

/**
 * 当前请求的用户上下文。
 * 由 AdminInterceptor 在每个请求开始时填充。
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> CTX = new ThreadLocal<>();

    public static void set(String userId, UserRole role) {
        CTX.set(new UserInfo(userId, role));
    }

    public static UserInfo get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }

    public static boolean isAdmin() {
        UserInfo info = CTX.get();
        return info != null && info.role == UserRole.ADMIN;
    }

    public static String currentUserId() {
        UserInfo info = CTX.get();
        return info != null ? info.userId : null;
    }

    public record UserInfo(String userId, UserRole role) {}
}
