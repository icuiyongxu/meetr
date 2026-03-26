package com.meetr.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口所需的权限码。
 * 配合 PermissionInterceptor 使用。
 * 示例：@RequirePermission("user:manage")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /** 所需权限码，如 "room:manage"、"booking:approve" */
    String value();
}
