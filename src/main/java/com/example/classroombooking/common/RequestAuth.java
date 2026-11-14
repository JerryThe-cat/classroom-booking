package com.example.classroombooking.common;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestAuth {
    private RequestAuth() {
    }

    public static Long userId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value instanceof Long id) {
            return id;
        }
        throw ApiException.unauthorized("未登录或登录已失效");
    }

    public static String role(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserRole");
        return value == null ? null : value.toString();
    }

    public static void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(role(request))) {
            throw ApiException.forbidden("仅管理员可执行该操作");
        }
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(role(request));
    }
}
