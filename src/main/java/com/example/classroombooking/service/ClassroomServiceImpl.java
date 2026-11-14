package com.example.classroombooking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.classroombooking.common.ApiException;
import com.example.classroombooking.common.TimeUtils;
import com.example.classroombooking.dto.ClassroomUpsertDTO;
import com.example.classroombooking.entity.Booking;
import com.example.classroombooking.entity.Classroom;
import com.example.classroombooking.mapper.BookingMapper;
import com.example.classroombooking.mapper.ClassroomMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class ClassroomServiceImpl implements ClassroomService {
    private static final Set<String> VALID_STATUS = Set.of("ACTIVE", "INACTIVE");

    private final ClassroomMapper classroomMapper;
    private final BookingMapper bookingMapper;
    private final long staleAfterSeconds;

    public ClassroomServiceImpl(
            ClassroomMapper classroomMapper,
            BookingMapper bookingMapper,
            @Value("${app.occupancy.stale-after-seconds:300}") long staleAfterSeconds
    ) {
        this.classroomMapper = classroomMapper;
        this.bookingMapper = bookingMapper;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    @Override
    public List<Classroom> list(boolean admin, String building, String status) {
        LambdaQueryWrapper<Classroom> query = new LambdaQueryWrapper<>();
        if (!admin) {
            query.eq(Classroom::getStatus, "ACTIVE");
        } else if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            validateStatus(status);
            query.eq(Classroom::getStatus, status.toUpperCase());
        }
        if (StringUtils.hasText(building)) {
            query.eq(Classroom::getBuilding, building.trim());
        }
        query.orderByAsc(Classroom::getBuilding).orderByAsc(Classroom::getRoomNo);
        List<Classroom> list = classroomMapper.selectList(query);
        list.forEach(this::enrichFreshness);
        return list;
    }

    @Override
    public Classroom get(Long id, boolean admin) {
        Classroom classroom = requireEntity(id);
        if (!admin && !"ACTIVE".equals(classroom.getStatus())) {
            throw ApiException.notFound("教室不存在或已停用");
        }
        enrichFreshness(classroom);
        return classroom;
    }

    @Override
    @Transactional
    public Classroom create(ClassroomUpsertDTO dto) {
        validateUniqueRoomNo(dto.getRoomNo(), null);
        Classroom classroom = new Classroom();
        apply(dto, classroom);
        classroom.setCurrentHeadCount(0);
        classroom.setOccupancyRate(java.math.BigDecimal.ZERO.setScale(2));
        classroomMapper.insert(classroom);
        enrichFreshness(classroom);
        return classroom;
    }

    @Override
    @Transactional
    public Classroom update(Long id, ClassroomUpsertDTO dto) {
        Classroom classroom = classroomMapper.selectByIdForUpdate(id);
        if (classroom == null) {
            throw ApiException.notFound("教室不存在");
        }
        validateUniqueRoomNo(dto.getRoomNo(), id);
        apply(dto, classroom);
        classroomMapper.updateById(classroom);
        enrichFreshness(classroom);
        return classroom;
    }

    @Override
    @Transactional
    public void disable(Long id) {
        Classroom classroom = classroomMapper.selectByIdForUpdate(id);
        if (classroom == null) {
            throw ApiException.notFound("教室不存在");
        }
        if ("INACTIVE".equals(classroom.getStatus())) {
            return;
        }

        LambdaQueryWrapper<Booking> activeQuery = new LambdaQueryWrapper<>();
        activeQuery.eq(Booking::getClassroomId, id)
                .in(Booking::getStatus, "PENDING", "APPROVED")
                .gt(Booking::getEndTime, TimeUtils.now());
        long count = bookingMapper.selectCount(activeQuery);
        if (count > 0) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "CLASSROOM_HAS_ACTIVE_BOOKINGS",
                    "该教室存在未结束预约，请先处理后再停用",
                    java.util.Map.of("unfinishedBookingCount", count)
            );
        }

        classroom.setStatus("INACTIVE");
        classroomMapper.updateById(classroom);
    }

    @Override
    public Classroom requireEntity(Long id) {
        Classroom classroom = classroomMapper.selectById(id);
        if (classroom == null) {
            throw ApiException.notFound("教室不存在");
        }
        return classroom;
    }

    private void validateUniqueRoomNo(String roomNo, Long excludeId) {
        LambdaQueryWrapper<Classroom> query = new LambdaQueryWrapper<>();
        query.eq(Classroom::getRoomNo, roomNo.trim());
        if (excludeId != null) {
            query.ne(Classroom::getId, excludeId);
        }
        if (classroomMapper.selectCount(query) > 0) {
            throw ApiException.conflict("DUPLICATE_RESOURCE", "教室编号已存在");
        }
    }

    private void apply(ClassroomUpsertDTO dto, Classroom classroom) {
        if (dto.getCapacity() < 1 || dto.getSeatCount() < 1) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "容纳人数和座位总数必须大于0");
        }
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().toUpperCase() : "ACTIVE";
        validateStatus(status);
        classroom.setRoomNo(dto.getRoomNo().trim());
        classroom.setBuilding(dto.getBuilding().trim());
        classroom.setCapacity(dto.getCapacity());
        classroom.setSeatCount(dto.getSeatCount());
        classroom.setEquipment(trimToNull(dto.getEquipment()));
        classroom.setStatus(status);
    }

    private void validateStatus(String status) {
        if (!VALID_STATUS.contains(status.toUpperCase())) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "教室状态只能为 ACTIVE 或 INACTIVE");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void enrichFreshness(Classroom classroom) {
        if (classroom.getOccupancyUpdatedAt() == null) {
            classroom.setOccupancyFresh(false);
            return;
        }
        long age = Duration.between(classroom.getOccupancyUpdatedAt(), TimeUtils.now()).getSeconds();
        classroom.setOccupancyFresh(age >= 0 && age <= staleAfterSeconds);
    }
}
