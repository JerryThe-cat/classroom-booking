package com.example.classroombooking.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtils {
    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private TimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(APP_ZONE);
    }

    public static LocalDateTime toLocal(OffsetDateTime time) {
        return time.atZoneSameInstant(APP_ZONE).toLocalDateTime();
    }
}
