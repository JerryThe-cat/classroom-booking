package com.example.classroombooking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.classroombooking.entity.Classroom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClassroomMapper extends BaseMapper<Classroom> {
    @Select("SELECT * FROM classroom WHERE id = #{id} FOR UPDATE")
    Classroom selectByIdForUpdate(@Param("id") Long id);
}
