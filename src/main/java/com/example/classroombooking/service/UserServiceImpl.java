package com.example.classroombooking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.classroombooking.common.ApiException;
import com.example.classroombooking.dto.LoginDTO;
import com.example.classroombooking.dto.RegisterDTO;
import com.example.classroombooking.entity.User;
import com.example.classroombooking.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterDTO dto) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, dto.getUsername().trim());
        if (userMapper.selectCount(query) > 0) {
            throw ApiException.conflict("USERNAME_EXISTS", "该账号已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName().trim());
        user.setRole("STUDENT");
        user.setDepartment(dto.getDepartment().trim());
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public User login(LoginDTO dto) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, dto.getUsername().trim());
        User user = userMapper.selectOne(query);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw ApiException.forbidden("该账号已被禁用");
        }
        user.setPasswordHash(null);
        return user;
    }

    @Override
    public User getActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.unauthorized("用户不存在或登录已失效");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw ApiException.forbidden("该账号已被禁用");
        }
        user.setPasswordHash(null);
        return user;
    }

    @Override
    @Transactional
    public void ensureAdmin(String username, String password, String name, String department) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("未配置 APP_ADMIN_USERNAME/APP_ADMIN_PASSWORD，跳过管理员初始化");
            return;
        }

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, username.trim());
        User existing = userMapper.selectOne(query);
        if (existing != null) {
            if (!"ADMIN".equals(existing.getRole())) {
                log.error("管理员初始化失败：账号 {} 已存在但不是 ADMIN", username);
            }
            return;
        }

        User admin = new User();
        admin.setUsername(username.trim());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setName(StringUtils.hasText(name) ? name.trim() : "系统管理员");
        admin.setRole("ADMIN");
        admin.setDepartment(StringUtils.hasText(department) ? department.trim() : "教务处");
        admin.setStatus("ACTIVE");
        userMapper.insert(admin);
        log.info("管理员账号 {} 初始化完成", username);
    }
}
