package com.example.classroombooking.service;

import com.example.classroombooking.common.PageResult;
import com.example.classroombooking.dto.BookingCreateDTO;
import com.example.classroombooking.entity.Booking;

import java.time.LocalDateTime;
import java.util.Map;

public interface BookingService {
    Map<String, Object> create(Long userId, BookingCreateDTO dto);

    PageResult<Booking> listMy(Long userId, String status, int page, int size);

    PageResult<Booking> listPending(int page, int size);

    PageResult<Booking> listAdmin(
            String status,
            Long classroomId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );

    Booking cancel(Long userId, Long bookingId);

    Booking audit(Long bookingId, Long approverId, String decision, String reason);

    Booking release(Long bookingId, String reason);

    int autoReleaseExpired();
}
