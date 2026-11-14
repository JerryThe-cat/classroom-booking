package com.example.classroombooking.controller;

import com.example.classroombooking.common.RequestAuth;
import com.example.classroombooking.common.Result;
import com.example.classroombooking.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(dashboardService.summary(from, to));
    }

    @GetMapping("/booking-frequency")
    public Result<List<Map<String, Object>>> bookingFrequency(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(dashboardService.bookingFrequency(from, to));
    }

    @GetMapping("/occupancy-series")
    public Result<Map<String, Object>> occupancySeries(
            @RequestParam Long classroomId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "60") int bucketMinutes,
            HttpServletRequest request
    ) {
        RequestAuth.requireAdmin(request);
        return Result.success(
                dashboardService.occupancySeries(classroomId, from, to, bucketMinutes)
        );
    }
}
