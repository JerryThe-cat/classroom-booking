package com.example.classroombooking.service;

import com.example.classroombooking.dto.LoginDTO;
import com.example.classroombooking.dto.RegisterDTO;
import com.example.classroombooking.entity.User;

public interface UserService {
    User register(RegisterDTO dto);

    User login(LoginDTO dto);

    User getActiveUser(Long userId);

    void ensureAdmin(String username, String password, String name, String department);
}
