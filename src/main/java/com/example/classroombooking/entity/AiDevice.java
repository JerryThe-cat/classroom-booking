package com.example.classroombooking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_device")
public class AiDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private Long classroomId;
    @JsonIgnore
    private String tokenHash;
    private String status;
    private Integer reportIntervalSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
