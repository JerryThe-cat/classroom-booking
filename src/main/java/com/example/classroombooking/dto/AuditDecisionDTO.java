package com.example.classroombooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditDecisionDTO {
    @NotBlank(message = "请选择审批决定")
    private String decision;

    @Size(max = 255, message = "审批意见不能超过255个字符")
    private String reason;
}
