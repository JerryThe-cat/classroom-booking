package com.example.classroombooking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("booking")
public class Booking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long classroomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private Integer expectedAttendees;
    private String status;
    private Long approverId;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime releasedAt;
    private String releaseReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String roomNo;
    @TableField(exist = false)
    private String building;
    @TableField(exist = false)
    private String approverName;
    @TableField(exist = false)
    private Boolean conflictWithApproved;
    @TableField(exist = false)
    private List<Long> conflictingBookingIds;
}
