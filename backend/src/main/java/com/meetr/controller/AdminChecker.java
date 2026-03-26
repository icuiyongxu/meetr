package com.meetr.controller;

import com.meetr.domain.UserContext;
import com.meetr.exception.BusinessException;

public class AdminChecker {

    public static void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(40301, "需要管理员权限");
        }
    }
}
