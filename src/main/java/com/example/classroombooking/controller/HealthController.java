package com.example.classroombooking.controller;

import com.example.classroombooking.common.Result;
import com.example.classroombooking.common.TimeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Result<Map<String, Object>>> health() {
        boolean databaseUp = false;
        try (Connection connection = dataSource.getConnection()) {
            databaseUp = connection.isValid(2);
        } catch (Exception ignored) {
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", databaseUp ? "UP" : "DOWN");
        data.put("database", databaseUp ? "UP" : "DOWN");
        data.put("time", OffsetDateTime.now(TimeUtils.APP_ZONE));

        if (databaseUp) {
            return ResponseEntity.ok(Result.success(data));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(503, "SERVICE_UNAVAILABLE", "数据库不可用", data));
    }
}
