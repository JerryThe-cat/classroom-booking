package com.example.classroombooking.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Object> summary(OffsetDateTime from, OffsetDateTime to);

    List<Map<String, Object>> bookingFrequency(OffsetDateTime from, OffsetDateTime to);

    Map<String, Object> occupancySeries(
            Long classroomId,
            OffsetDateTime from,
            OffsetDateTime to,
            int bucketMinutes
    );
}
