package com.example.classroombooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BookingCreateDTO {
    @NotNull(message = "请选择教室")
    private Long classroomId;

    @NotNull(message = "请选择开始时间")
    private OffsetDateTime startTime;

    @NotNull(message = "请选择结束时间")
    private OffsetDateTime endTime;

    @NotBlank(message = "请填写预约用途")
    @Size(max = 255, message = "预约用途不能超过255个字符")
    private String purpose;

    @NotNull(message = "请填写预计人数")
    @Min(value = 1, message = "预计人数至少为1")
    private Integer expectedAttendees;
}
