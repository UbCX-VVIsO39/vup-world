package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录/注册接口的内存级速率限制。
 * 对 /api/auth/login、/api/auth/register 限制每 IP 每分钟 5 次，超限返回 429。
 * 在 RequestIdFilter（HIGHEST_PRECEDENCE）之后执行，复用其注入的 requestId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int DEFAULT_MAX_REQUESTS_PER_WINDOW = 5;
    private static final long DEFAULT_WINDOW_MILLIS = 60_000L;
    // 仅对写接口限速，避免影响鉴权状态读取等只读接口
    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/auth/register");

    private final Map<String, Deque<Long>> accessRecords = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxRequestsPerWindow;
    private final long windowMillis;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${vupworld.rate-limit.auth.enabled:true}") boolean enabled,
            @Value("${vupworld.rate-limit.auth.max-requests-per-window:5}") int maxRequestsPerWindow,
            @Value("${vupworld.rate-limit.auth.window-millis:60000}") long windowMillis
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxRequestsPerWindow = Math.max(1, maxRequestsPerWindow);
        this.windowMillis = Math.max(1L, windowMillis);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (enabled && isLimited(request)) {
            String key = clientIp(request) + "|" + request.getRequestURI();
            if (isRateLimited(key)) {
                writeRateLimited(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLimited(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && LIMITED_PATHS.contains(request.getRequestURI());
    }

    // 滑动窗口判定：返回 true 表示已超限
    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = accessRecords.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            // 清理超过 1 分钟的旧记录
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMillis) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequestsPerWindow) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }

    private void writeRateLimited(HttpServletResponse response) throws IOException {
        ApiResponse<Object> body = ApiResponse.error("RATE_LIMITED", "请求过于频繁，请稍后再试", null);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 取链中第一个 IP（最原始客户端）
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
