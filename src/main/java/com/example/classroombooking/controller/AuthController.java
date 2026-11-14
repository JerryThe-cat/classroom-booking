package com.example.classroombooking.controller;

import com.example.classroombooking.common.RequestAuth;
import com.example.classroombooking.common.Result;
import com.example.classroombooking.dto.LoginDTO;
import com.example.classroombooking.dto.RegisterDTO;
import com.example.classroombooking.entity.User;
import com.example.classroombooking.service.UserService;
import com.example.classroombooking.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtils jwtUtils;

    public AuthController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto) {
        User user = userService.register(dto);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return Result.success(data);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        User user = userService.login(dto);
        String token = jwtUtils.generateToken(user.getId(), user.getRole());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("expiresAt", jwtUtils.expirationTime()
                .atZone(com.example.classroombooking.common.TimeUtils.APP_ZONE)
                .toOffsetDateTime());
        data.put("user", user);
        return Result.success(data);
    }

    @GetMapping("/me")
    public Result<User> me(HttpServletRequest request) {
        return Result.success(userService.getActiveUser(RequestAuth.userId(request)));
    }
}
