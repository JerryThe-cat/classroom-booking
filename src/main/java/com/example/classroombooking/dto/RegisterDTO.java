package com.example.classroombooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @NotBlank(message = "请输入账号")
    @Size(max = 50, message = "账号不能超过50个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 8, max = 72, message = "密码长度必须为8到72个字符")
    private String password;

    @NotBlank(message = "请输入姓名")
    @Size(max = 50, message = "姓名不能超过50个字符")
    private String name;

    @NotBlank(message = "请输入院系")
    @Size(max = 100, message = "院系不能超过100个字符")
    private String department;
}
