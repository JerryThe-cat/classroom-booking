package com.example.classroombooking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("classroom")
public class Classroom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomNo;
    private String building;
    private Integer capacity;
    private Integer seatCount;
    private String equipment;
    private Integer currentHeadCount;
    @TableField("current_occupancy_rate")
    private BigDecimal occupancyRate;
    private LocalDateTime occupancyUpdatedAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Boolean occupancyFresh;
}
