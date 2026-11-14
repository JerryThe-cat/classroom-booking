package com.example.classroombooking.utils;

import com.example.classroombooking.common.ApiException;
import com.example.classroombooking.entity.User;
import com.example.classroombooking.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    public JwtInterceptor(JwtUtils jwtUtils, UserMapper userMapper) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw ApiException.unauthorized("未登录或 Token 缺失");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw ApiException.unauthorized("未登录或 Token 缺失");
        }

        Claims claims = jwtUtils.parseToken(token);
        Long userId = Long.valueOf(claims.get("userId").toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.unauthorized("用户不存在或登录已失效");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw ApiException.forbidden("账号已被禁用");
        }

        request.setAttribute("currentUserId", user.getId());
        request.setAttribute("currentUserRole", user.getRole());
        return true;
    }
}
