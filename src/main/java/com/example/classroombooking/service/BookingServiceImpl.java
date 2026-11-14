package com.example.classroombooking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.classroombooking.common.ApiException;
import com.example.classroombooking.common.PageResult;
import com.example.classroombooking.common.TimeUtils;
import com.example.classroombooking.dto.BookingCreateDTO;
import com.example.classroombooking.entity.Booking;
import com.example.classroombooking.entity.Classroom;
import com.example.classroombooking.entity.User;
import com.example.classroombooking.mapper.BookingMapper;
import com.example.classroombooking.mapper.ClassroomMapper;
import com.example.classroombooking.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final Set<String> BOOKING_STATUS = Set.of(
            "PENDING", "APPROVED", "REJECTED", "CANCELLED", "RELEASED"
    );

    private final BookingMapper bookingMapper;
    private final ClassroomMapper classroomMapper;
    private final UserMapper userMapper;

    public BookingServiceImpl(
            BookingMapper bookingMapper,
            ClassroomMapper classroomMapper,
            UserMapper userMapper
    ) {
        this.bookingMapper = bookingMapper;
        this.classroomMapper = classroomMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> create(Long userId, BookingCreateDTO dto) {
        LocalDateTime start = TimeUtils.toLocal(dto.getStartTime());
        LocalDateTime end = TimeUtils.toLocal(dto.getEndTime());
        validateTime(start, end);

        Classroom classroom = classroomMapper.selectByIdForUpdate(dto.getClassroomId());
        if (classroom == null || !"ACTIVE".equals(classroom.getStatus())) {
            throw ApiException.notFound("教室不存在或已停用");
        }
        if (dto.getExpectedAttendees() > classroom.getCapacity()) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "预计人数不能超过教室容纳人数");
        }

        long approvedOverlaps = bookingMapper.countOverlaps(
                classroom.getId(), "APPROVED", start, end, null
        );
        if (approvedOverlaps > 0) {
            throw ApiException.conflict(
                    "BOOKING_CONFLICT_APPROVED",
                    "该教室在所选时段已有审批通过的预约"
            );
        }

        long pendingOverlaps = bookingMapper.countOverlaps(
                classroom.getId(), "PENDING", start, end, null
        );

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setClassroomId(classroom.getId());
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setPurpose(dto.getPurpose().trim());
        booking.setExpectedAttendees(dto.getExpectedAttendees());
        booking.setStatus("PENDING");
        bookingMapper.insert(booking);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookingId", booking.getId());
        result.put("status", booking.getStatus());
        result.put("conflictWithPending", pendingOverlaps > 0);
        return result;
    }

    @Override
    public PageResult<Booking> listMy(Long userId, String status, int page, int size) {
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<>();
        query.eq(Booking::getUserId, userId);
        applyStatus(query, status);
        query.orderByDesc(Booking::getStartTime);
        List<Booking> list = bookingMapper.selectList(query);
        enrich(list);
        return PageResult.of(list, page, size);
    }

    @Override
    public PageResult<Booking> listPending(int page, int size) {
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<>();
        query.eq(Booking::getStatus, "PENDING")
                .orderByAsc(Booking::getCreatedAt);
        List<Booking> list = bookingMapper.selectList(query);
        enrich(list);
        list.forEach(this::attachConflicts);
        return PageResult.of(list, page, size);
    }

    @Override
    public PageResult<Booking> listAdmin(
            String status,
            Long classroomId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<>();
        applyStatus(query, status);
        if (classroomId != null) {
            query.eq(Booking::getClassroomId, classroomId);
        }
        if (from != null) {
            query.ge(Booking::getStartTime, from);
        }
        if (to != null) {
            query.le(Booking::getStartTime, to);
        }
        query.orderByDesc(Booking::getStartTime);
        List<Booking> list = bookingMapper.selectList(query);
        enrich(list);
        return PageResult.of(list, page, size);
    }

    @Override
    @Transactional
    public Booking cancel(Long userId, Long bookingId) {
        Booking booking = bookingMapper.selectByIdForUpdate(bookingId);
        if (booking == null) {
            throw ApiException.notFound("预约记录不存在");
        }
        if (!booking.getUserId().equals(userId)) {
            throw ApiException.forbidden("无权取消他人的预约");
        }
        if (!Set.of("PENDING", "APPROVED").contains(booking.getStatus())) {
            throw ApiException.conflict("BOOKING_STATE_CONFLICT", "当前状态不支持取消");
        }
        if (!booking.getStartTime().isAfter(TimeUtils.now())) {
            throw ApiException.conflict("BOOKING_STATE_CONFLICT", "该预约已经开始，无法取消");
        }

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(TimeUtils.now());
        bookingMapper.updateById(booking);
        enrich(booking);
        return booking;
    }

    @Override
    @Transactional
    public Booking audit(Long bookingId, Long approverId, String decision, String reason) {
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        if (!Set.of("APPROVE", "REJECT").contains(normalized)) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "审批决定只能为 APPROVE 或 REJECT");
        }
        if ("REJECT".equals(normalized) && !StringUtils.hasText(reason)) {
            throw ApiException.unprocessable("REJECT_REASON_REQUIRED", "驳回时必须填写理由");
        }

        Booking booking = bookingMapper.selectByIdForUpdate(bookingId);
        if (booking == null) {
            throw ApiException.notFound("预约记录不存在");
        }
        if (!"PENDING".equals(booking.getStatus())) {
            throw ApiException.conflict("BOOKING_STATE_CONFLICT", "该预约已被处理过");
        }

        if ("APPROVE".equals(normalized)) {
            Classroom classroom = classroomMapper.selectByIdForUpdate(booking.getClassroomId());
            if (classroom == null || !"ACTIVE".equals(classroom.getStatus())) {
                throw ApiException.conflict("BOOKING_STATE_CONFLICT", "教室已停用，无法审批通过");
            }
            long overlap = bookingMapper.countOverlaps(
                    booking.getClassroomId(),
                    "APPROVED",
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getId()
            );
            if (overlap > 0) {
                throw ApiException.conflict(
                        "BOOKING_CONFLICT_APPROVED",
                        "该时段已有其他审批通过的预约"
                );
            }
            booking.setStatus("APPROVED");
            booking.setRejectReason(null);
        } else {
            booking.setStatus("REJECTED");
            booking.setRejectReason(reason.trim());
        }

        booking.setApproverId(approverId);
        booking.setReviewedAt(TimeUtils.now());
        bookingMapper.updateById(booking);
        enrich(booking);
        return booking;
    }

    @Override
    @Transactional
    public Booking release(Long bookingId, String reason) {
        Booking booking = bookingMapper.selectByIdForUpdate(bookingId);
        if (booking == null) {
            throw ApiException.notFound("预约记录不存在");
        }
        if (!Set.of("PENDING", "APPROVED").contains(booking.getStatus())) {
            throw ApiException.conflict("BOOKING_STATE_CONFLICT", "当前状态不支持人工释放");
        }
        booking.setStatus("RELEASED");
        booking.setReleasedAt(TimeUtils.now());
        booking.setReleaseReason(reason.trim());
        bookingMapper.updateById(booking);
        enrich(booking);
        return booking;
    }

    @Override
    @Transactional
    public int autoReleaseExpired() {
        LocalDateTime now = TimeUtils.now();
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<>();
        query.in(Booking::getStatus, "PENDING", "APPROVED")
                .lt(Booking::getEndTime, now);
        List<Booking> expired = bookingMapper.selectList(query);

        for (Booking booking : expired) {
            String previousStatus = booking.getStatus();
            booking.setStatus("RELEASED");
            booking.setReleasedAt(now);
            booking.setReleaseReason(
                    "PENDING".equals(previousStatus) ? "AUTO_EXPIRED" : "AUTO_ENDED"
            );
            bookingMapper.updateById(booking);
        }
        if (!expired.isEmpty()) {
            log.info("自动释放预约 {} 条", expired.size());
        }
        return expired.size();
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw ApiException.unprocessable("INVALID_BOOKING_TIME", "开始时间必须早于结束时间");
        }
        if (!start.isAfter(TimeUtils.now())) {
            throw ApiException.unprocessable("INVALID_BOOKING_TIME", "不能预约过去的时间");
        }
    }

    private void applyStatus(LambdaQueryWrapper<Booking> query, String status) {
        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status)) {
            return;
        }
        String normalized = status.toUpperCase();
        if (!BOOKING_STATUS.contains(normalized)) {
            throw ApiException.unprocessable("VALIDATION_ERROR", "预约状态参数非法");
        }
        query.eq(Booking::getStatus, normalized);
    }

    private void enrich(List<Booking> bookings) {
        bookings.forEach(this::enrich);
    }

    private void enrich(Booking booking) {
        User user = userMapper.selectById(booking.getUserId());
        if (user != null) {
            booking.setUserName(user.getName());
        }
        Classroom classroom = classroomMapper.selectById(booking.getClassroomId());
        if (classroom != null) {
            booking.setRoomNo(classroom.getRoomNo());
            booking.setBuilding(classroom.getBuilding());
        }
        if (booking.getApproverId() != null) {
            User approver = userMapper.selectById(booking.getApproverId());
            if (approver != null) {
                booking.setApproverName(approver.getName());
            }
        }
    }

    private void attachConflicts(Booking booking) {
        LambdaQueryWrapper<Booking> query = new LambdaQueryWrapper<>();
        query.eq(Booking::getClassroomId, booking.getClassroomId())
                .ne(Booking::getId, booking.getId())
                .in(Booking::getStatus, "PENDING", "APPROVED")
                .lt(Booking::getStartTime, booking.getEndTime())
                .gt(Booking::getEndTime, booking.getStartTime())
                .orderByAsc(Booking::getCreatedAt);
        List<Booking> conflicts = bookingMapper.selectList(query);
        booking.setConflictingBookingIds(conflicts.stream().map(Booking::getId).toList());
        booking.setConflictWithApproved(
                conflicts.stream().anyMatch(item -> "APPROVED".equals(item.getStatus()))
        );
    }
}
