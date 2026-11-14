package com.example.classroombooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReleaseDTO {
    @NotBlank(message = "请填写释放原因")
    @Size(max = 255, message = "释放原因不能超过255个字符")
    private String reason;
}
