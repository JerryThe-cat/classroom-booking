package com.example.classroombooking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.classroombooking.common.ApiException;
import com.example.classroombooking.common.TimeUtils;
import com.example.classroombooking.dto.OccupancyReportDTO;
import com.example.classroombooking.entity.AiDevice;
import com.example.classroombooking.entity.Classroom;
import com.example.classroombooking.entity.OccupancyRecord;
import com.example.classroombooking.mapper.AiDeviceMapper;
import com.example.classroombooking.mapper.ClassroomMapper;
import com.example.classroombooking.mapper.OccupancyRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class OccupancyServiceImpl implements OccupancyService {
    private static final Set<String> VALID_SOURCE = Set.of("REAL", "SIMULATED");

    private final AiDeviceMapper aiDeviceMapper;
    private final ClassroomMapper classroomMapper;
    private final OccupancyRecordMapper occupancyRecordMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long staleAfterSeconds;

    public OccupancyServiceImpl(
            AiDeviceMapper aiDeviceMapper,
            ClassroomMapper classroomMapper,
            OccupancyRecordMapper occupancyRecordMapper,
            BCryptPasswordEncoder passwordEncoder,
            @Value("${app.occupancy.stale-after-seconds:300}") long staleAfterSeconds
    ) {
        this.aiDeviceMapper = aiDeviceMapper;
        this.classroomMapper = classroomMapper;
        this.occupancyRecordMapper = occupancyRecordMapper;
        this.passwordEncoder = passwordEncoder;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    @Override
    public AiDevice authenticate(String deviceId, String deviceToken) {
        if (deviceId == null || deviceId.isBlank() || deviceToken == null || deviceToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "DEVICE_UNAUTHORIZED", "缺少设备凭据");
        }
        LambdaQueryWrapper<AiDevice> query = new LambdaQueryWrapper<>();
        query.eq(AiDevice::getDeviceId, deviceId);
        AiDevice device = aiDeviceMapper.selectOne(query);
        if (device == null
                || !"ACTIVE".equals(device.getStatus())
                || !passwordEncoder.matches(deviceToken, device.getTokenHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "DEVICE_UNAUTHORIZED", "设备凭据无效");
        }
        return device;
    }

    @Override
    public Map<String, Object> config(String deviceId, String deviceToken) {
        AiDevice device = authenticate(deviceId, deviceToken);
        Classroom classroom = classroomMapper.selectById(device.getClassroomId());
        if (classroom == null || !"ACTIVE".equals(classroom.getStatus())) {
            throw ApiException.notFound("设备绑定的教室不存在或已停用");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classroomId", classroom.getId());
        result.put("roomNo", classroom.getRoomNo());
        result.put("seatCount", classroom.getSeatCount());
        result.put("reportIntervalSeconds", device.getReportIntervalSeconds());
        result.put("staleAfterSeconds", staleAfterSeconds);
        result.put("serverTime", OffsetDateTime.now(TimeUtils.APP_ZONE));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> report(String deviceId, String deviceToken, OccupancyReportDTO dto) {
        AiDevice device = authenticate(deviceId, deviceToken);
        if (!deviceId.equals(dto.getDeviceId()) || !device.getClassroomId().equals(dto.getClassroomId())) {
            throw ApiException.unprocessable("OCCUPANCY_OUT_OF_ORDER", "设备与教室绑定关系不一致");
        }
        if (!VALID_SOURCE.contains(dto.getSource())) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "source 只能为 REAL 或 SIMULATED");
        }

        LocalDateTime collectedAt = TimeUtils.toLocal(dto.getCollectedAt());
        LocalDateTime now = TimeUtils.now();
        if (collectedAt.isAfter(now.plusMinutes(5))) {
            throw ApiException.unprocessable("OCCUPANCY_OUT_OF_ORDER", "采集时间不能明显晚于服务器时间");
        }

        Classroom classroom = classroomMapper.selectByIdForUpdate(dto.getClassroomId());
        if (classroom == null || !"ACTIVE".equals(classroom.getStatus())) {
            throw ApiException.notFound("教室不存在或已停用");
        }
        if (classroom.getSeatCount() == null || classroom.getSeatCount() <= 0) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "教室座位总数未正确配置");
        }

        BigDecimal computedRate = BigDecimal.valueOf(dto.getHeadCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(classroom.getSeatCount()), 2, RoundingMode.HALF_UP);
        BigDecimal clientRate = dto.getOccupancyRate().setScale(2, RoundingMode.HALF_UP);
        if (computedRate.subtract(clientRate).abs().compareTo(new BigDecimal("0.05")) > 0) {
            throw ApiException.unprocessable(
                    "OCCUPANCY_RATE_MISMATCH",
                    "客户端占用率与后端按座位数计算的结果不一致"
            );
        }

        LambdaQueryWrapper<OccupancyRecord> duplicateQuery = new LambdaQueryWrapper<>();
        duplicateQuery.eq(OccupancyRecord::getDeviceId, deviceId)
                .eq(OccupancyRecord::getSequenceNo, dto.getSequence());
        OccupancyRecord existing = occupancyRecordMapper.selectOne(duplicateQuery);
        if (existing != null) {
            return response(existing.getId(), false);
        }

        OccupancyRecord record = new OccupancyRecord();
        record.setClassroomId(classroom.getId());
        record.setHeadCount(dto.getHeadCount());
        record.setSeatCount(classroom.getSeatCount());
        record.setOccupancyRate(computedRate);
        record.setCollectedAt(collectedAt);
        record.setSource(dto.getSource());
        record.setDeviceId(deviceId);
        record.setSequenceNo(dto.getSequence());
        occupancyRecordMapper.insert(record);

        boolean currentUpdated = classroom.getOccupancyUpdatedAt() == null
                || !collectedAt.isBefore(classroom.getOccupancyUpdatedAt());
        if (currentUpdated) {
            classroom.setCurrentHeadCount(dto.getHeadCount());
            classroom.setOccupancyRate(computedRate);
            classroom.setOccupancyUpdatedAt(collectedAt);
            classroomMapper.updateById(classroom);
        }
        return response(record.getId(), currentUpdated);
    }

    private Map<String, Object> response(Long recordId, boolean currentUpdated) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", recordId);
        result.put("currentUpdated", currentUpdated);
        result.put("serverTime", OffsetDateTime.now(TimeUtils.APP_ZONE));
        return result;
    }
}
