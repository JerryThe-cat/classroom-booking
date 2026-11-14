package com.example.classroombooking.controller;

import com.example.classroombooking.common.Result;
import com.example.classroombooking.dto.OccupancyReportDTO;
import com.example.classroombooking.service.OccupancyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final OccupancyService occupancyService;

    public AiController(OccupancyService occupancyService) {
        this.occupancyService = occupancyService;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken
    ) {
        return Result.success(occupancyService.config(deviceId, deviceToken));
    }

    @PostMapping("/occupancy/report")
    public Result<Map<String, Object>> report(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody OccupancyReportDTO dto
    ) {
        return Result.success(occupancyService.report(deviceId, deviceToken, dto));
    }
}
