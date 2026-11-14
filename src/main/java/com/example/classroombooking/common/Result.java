package com.example.classroombooking.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private int code;
    private String errorCode;
    private String message;
    private T data;
    private OffsetDateTime timestamp;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, null, "success", data, now());
    }

    public static <T> Result<T> success(int code, T data) {
        return new Result<>(code, null, "success", data, now());
    }

    public static <T> Result<T> error(int code, String errorCode, String message) {
        return new Result<>(code, errorCode, message, null, now());
    }

    public static <T> Result<T> error(int code, String errorCode, String message, T data) {
        return new Result<>(code, errorCode, message, data, now());
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneId.of("Asia/Shanghai"));
    }
}
