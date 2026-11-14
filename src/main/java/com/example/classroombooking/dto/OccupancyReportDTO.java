package com.example.classroombooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class OccupancyReportDTO {
    @NotNull(message = "classroomId不能为空")
    private Long classroomId;

    @NotBlank(message = "deviceId不能为空")
    private String deviceId;

    @NotNull(message = "headCount不能为空")
    @Min(value = 0, message = "headCount不能小于0")
    private Integer headCount;

    @NotNull(message = "occupancyRate不能为空")
    private BigDecimal occupancyRate;

    @NotNull(message = "collectedAt不能为空")
    private OffsetDateTime collectedAt;

    @NotBlank(message = "source不能为空")
    private String source;

    @NotNull(message = "sequence不能为空")
    @Min(value = 1, message = "sequence必须大于0")
    private Long sequence;
}
