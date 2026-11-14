package com.example.classroombooking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.classroombooking.common.ApiException;
import com.example.classroombooking.common.TimeUtils;
import com.example.classroombooking.entity.Booking;
import com.example.classroombooking.entity.Classroom;
import com.example.classroombooking.entity.OccupancyRecord;
import com.example.classroombooking.mapper.BookingMapper;
import com.example.classroombooking.mapper.ClassroomMapper;
import com.example.classroombooking.mapper.OccupancyRecordMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final BookingMapper bookingMapper;
    private final ClassroomMapper classroomMapper;
    private final OccupancyRecordMapper occupancyRecordMapper;

    public DashboardServiceImpl(
            BookingMapper bookingMapper,
            ClassroomMapper classroomMapper,
            OccupancyRecordMapper occupancyRecordMapper
    ) {
        this.bookingMapper = bookingMapper;
        this.classroomMapper = classroomMapper;
        this.occupancyRecordMapper = occupancyRecordMapper;
    }

    @Override
    public Map<String, Object> summary(OffsetDateTime from, OffsetDateTime to) {
        DateRange range = range(from, to);
        List<Booking> bookings = bookings(range);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("PENDING", 0L);
        counts.put("APPROVED", 0L);
        counts.put("RELEASED", 0L);
        counts.put("CANCELLED", 0L);
        counts.put("REJECTED", 0L);
        bookings.forEach(b -> counts.computeIfPresent(b.getStatus(), (key, value) -> value + 1));

        List<Map<String, Object>> top = bookingFrequency(range).stream()
                .filter(item -> ((Long) item.get("bookingCount")) > 0)
                .sorted(Comparator.comparingLong(item -> -(Long) item.get("bookingCount")))
                .limit(5)
                .map(item -> {
                    Map<String, Object> one = new LinkedHashMap<>();
                    one.put("classroomId", item.get("classroomId"));
                    one.put("roomNo", item.get("roomNo"));
                    one.put("bookingCount", item.get("bookingCount"));
                    return one;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalBookings", bookings.size());
        result.put("pendingBookings", counts.get("PENDING"));
        result.put("approvedBookings", counts.get("APPROVED"));
        result.put("releasedBookings", counts.get("RELEASED"));
        result.put("cancelledBookings", counts.get("CANCELLED"));
        result.put("rejectedBookings", counts.get("REJECTED"));
        result.put("from", range.from().atZone(TimeUtils.APP_ZONE).toOffsetDateTime());
        result.put("to", range.to().atZone(TimeUtils.APP_ZONE).toOffsetDateTime());
        result.put("topClassrooms", top);
        return result;
    }

    @Override
    public List<Map<String, Object>> bookingFrequency(OffsetDateTime from, OffsetDateTime to) {
        return bookingFrequency(range(from, to));
    }

    @Override
    public Map<String, Object> occupancySeries(
            Long classroomId,
            OffsetDateTime from,
            OffsetDateTime to,
            int bucketMinutes
    ) {
        if (bucketMinutes < 1 || bucketMinutes > 1440) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "bucketMinutes 必须在 1 到 1440 之间");
        }
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw ApiException.notFound("教室不存在");
        }
        DateRange range = range(from, to);
        LambdaQueryWrapper<OccupancyRecord> query = new LambdaQueryWrapper<>();
        query.eq(OccupancyRecord::getClassroomId, classroomId)
                .ge(OccupancyRecord::getCollectedAt, range.from())
                .le(OccupancyRecord::getCollectedAt, range.to())
                .orderByAsc(OccupancyRecord::getCollectedAt);
        List<OccupancyRecord> records = occupancyRecordMapper.selectList(query);

        long bucketSeconds = bucketMinutes * 60L;
        TreeMap<Long, List<OccupancyRecord>> buckets = new TreeMap<>();
        for (OccupancyRecord record : records) {
            long epoch = record.getCollectedAt().atZone(TimeUtils.APP_ZONE).toEpochSecond();
            long bucket = epoch - Math.floorMod(epoch, bucketSeconds);
            buckets.computeIfAbsent(bucket, key -> new ArrayList<>()).add(record);
        }

        List<Map<String, Object>> points = new ArrayList<>();
        for (Map.Entry<Long, List<OccupancyRecord>> entry : buckets.entrySet()) {
            List<OccupancyRecord> values = entry.getValue();
            double avgHead = values.stream().mapToInt(OccupancyRecord::getHeadCount).average().orElse(0);
            int maxHead = values.stream().mapToInt(OccupancyRecord::getHeadCount).max().orElse(0);
            BigDecimal avgRate = values.stream()
                    .map(OccupancyRecord::getOccupancyRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("bucketStart", java.time.Instant.ofEpochSecond(entry.getKey())
                    .atZone(TimeUtils.APP_ZONE).toOffsetDateTime());
            point.put("avgHeadCount", BigDecimal.valueOf(avgHead).setScale(2, RoundingMode.HALF_UP));
            point.put("maxHeadCount", maxHead);
            point.put("avgOccupancyRate", avgRate);
            points.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classroomId", classroom.getId());
        result.put("roomNo", classroom.getRoomNo());
        result.put("bucketMinutes", bucketMinutes);
        result.put("points", points);
        return result;
    }

    private List<Map<String, Object>> bookingFrequency(DateRange range) {
        List<Booking> bookings = bookings(range);
        List<Classroom> classrooms = classroomMapper.selectList(null);
        Map<Long, Long> total = new LinkedHashMap<>();
        Map<Long, Long> approved = new LinkedHashMap<>();
        Map<Long, Long> released = new LinkedHashMap<>();
        for (Booking booking : bookings) {
            total.merge(booking.getClassroomId(), 1L, Long::sum);
            if ("APPROVED".equals(booking.getStatus())) {
                approved.merge(booking.getClassroomId(), 1L, Long::sum);
            }
            if ("RELEASED".equals(booking.getStatus())) {
                released.merge(booking.getClassroomId(), 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Classroom classroom : classrooms) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("classroomId", classroom.getId());
            item.put("roomNo", classroom.getRoomNo());
            item.put("bookingCount", total.getOrDefault(classroom.getId(), 0L));
            item.put("approvedCount", approved.getOrDefault(classroom.getId(), 0L));
            item.put("releasedCount", released.getOrDefault(classroom.getId(), 0L));
            result.add(item);
        }
        return result;
    }

    private List<Booking> bookings(DateRange range) {
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<>();
        query.ge(Booking::getStartTime, range.from())
                .le(Booking::getStartTime, range.to());
        return bookingMapper.selectList(query);
    }

    private DateRange range(OffsetDateTime from, OffsetDateTime to) {
        LocalDateTime end = to == null
                ? TimeUtils.now()
                : TimeUtils.toLocal(to);
        LocalDateTime start = from == null
                ? end.minusDays(30)
                : TimeUtils.toLocal(from);
        if (!start.isBefore(end)) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "开始时间必须早于结束时间");
        }
        return new DateRange(start, end);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }
}
