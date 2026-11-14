package com.example.classroombooking.service;

import com.example.classroombooking.dto.OccupancyReportDTO;
import com.example.classroombooking.entity.AiDevice;

import java.util.Map;

public interface OccupancyService {
    AiDevice authenticate(String deviceId, String deviceToken);

    Map<String, Object> config(String deviceId, String deviceToken);

    Map<String, Object> report(String deviceId, String deviceToken, OccupancyReportDTO dto);
}
