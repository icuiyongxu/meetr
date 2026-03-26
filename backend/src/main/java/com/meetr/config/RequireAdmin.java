package com.meetr.config;

import com.meetr.domain.UserContext;
import com.meetr.exception.BusinessException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要管理员权限的接口。
 * 在 Controller 方法上使用，配合 AdminInterceptor 或 AOP 使用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
