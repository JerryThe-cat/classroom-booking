package com.example.classroombooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassroomUpsertDTO {
    @NotBlank(message = "请输入教室编号")
    @Size(max = 50, message = "教室编号不能超过50个字符")
    private String roomNo;

    @NotBlank(message = "请输入楼栋")
    @Size(max = 100, message = "楼栋不能超过100个字符")
    private String building;

    @Min(value = 1, message = "容纳人数必须大于0")
    private int capacity;

    @Min(value = 1, message = "座位总数必须大于0")
    private int seatCount;

    @Size(max = 255, message = "设备信息不能超过255个字符")
    private String equipment;

    private String status = "ACTIVE";
}
