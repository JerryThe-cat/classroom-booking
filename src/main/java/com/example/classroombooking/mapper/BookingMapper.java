package com.example.classroombooking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.classroombooking.entity.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {
    @Select("SELECT * FROM booking WHERE id = #{id} FOR UPDATE")
    Booking selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT COUNT(*)
            FROM booking
            WHERE classroom_id = #{classroomId}
              AND status = #{status}
              AND start_time < #{endTime}
              AND end_time > #{startTime}
              AND (#{excludeId} IS NULL OR id <> #{excludeId})
            """)
    long countOverlaps(
            @Param("classroomId") Long classroomId,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId
    );
}
