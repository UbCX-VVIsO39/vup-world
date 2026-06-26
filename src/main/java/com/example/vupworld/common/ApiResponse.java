package com.example.vupworld.common;

import org.slf4j.MDC;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String requestId
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "OK", message, data, resolveRequestId());
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data, resolveRequestId());
    }

    /**
     * 优先复用 RequestIdFilter 注入 MDC 的 requestId，避免同一请求生成多个 id；
     * MDC 缺失时（如非 HTTP 上下文）回退到本地生成。
     */
    private static String resolveRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : newRequestId();
    }

    private static String newRequestId() {
        return DateTimeFormatter.BASIC_ISO_DATE.format(java.time.LocalDate.now())
                + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }
}
