package com.example.classroombooking.controller;

import com.example.classroombooking.common.PageResult;
import com.example.classroombooking.common.RequestAuth;
import com.example.classroombooking.common.Result;
import com.example.classroombooking.common.TimeUtils;
import com.example.classroombooking.dto.AuditDecisionDTO;
import com.example.classroombooking.dto.BookingCreateDTO;
import com.example.classroombooking.dto.ReleaseDTO;
import com.example.classroombooking.entity.Booking;
import com.example.classroombooking.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public Result<Map<String, Object>> create(
            @Valid @RequestBody BookingCreateDTO dto,
            HttpServletRequest request
    ) {
        return Result.success(bookingService.create(RequestAuth.userId(request), dto));
    }

    @GetMapping("/my")
    public Result<PageResult<Booking>> myBookings(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return Result.success(
                bookingService.listMy(RequestAuth.userId(request), status, page, size)
        );
    }

    @PutMapping("/{id}/cancel")
    public Result<Booking> cancel(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(
                bookingService.cancel(RequestAuth.userId(request), id)
        );
    }

    @GetMapping("/admin/pending")
    public Result<PageResult<Booking>> pending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(bookingService.listPending(page, size));
    }

    @GetMapping("/admin")
    public Result<PageResult<Booking>> all(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(bookingService.listAdmin(
                status,
                classroomId,
                from == null ? null : TimeUtils.toLocal(from),
                to == null ? null : TimeUtils.toLocal(to),
                page,
                size
        ));
    }

    @PutMapping("/admin/{id}/audit")
    public Result<Booking> audit(
            @PathVariable Long id,
            @Valid @RequestBody AuditDecisionDTO dto,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(bookingService.audit(
                id,
                RequestAuth.userId(request),
                dto.getDecision(),
                dto.getReason()
        ));
    }

    @PutMapping("/admin/{id}/release")
    public Result<Booking> release(
            @PathVariable Long id,
            @Valid @RequestBody ReleaseDTO dto,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(bookingService.release(id, dto.getReason()));
    }
}
