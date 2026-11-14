package com.example.classroombooking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("occupancy_record")
public class OccupancyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classroomId;
    private Integer headCount;
    private Integer seatCount;
    private BigDecimal occupancyRate;
    private LocalDateTime collectedAt;
    private String source;
    private String deviceId;
    private Long sequenceNo;
    private LocalDateTime createdAt;
}
