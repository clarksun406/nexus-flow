package com.nexusflow.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter using a sliding-window counter.
 *
 * Limits each client IP to a configurable number of requests per minute.
 * Endpoints under /actuator and /cashier are excluded.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequestsPerMinute;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(int maxRequestsPerMinute, ObjectMapper objectMapper) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();
        long currentMinute = now / 60_000;

        WindowCounter counter = counters.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new WindowCounter(currentMinute);
            }
            return existing;
        });

        int count = counter.count.incrementAndGet();
        if (count > maxRequestsPerMinute) {
            log.warn("Rate limit exceeded for IP: {} ({} requests this minute)", clientIp, count);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.fail(ErrorCode.INVALID_REQUEST, "Rate limit exceeded. Try again later.")));
            return;
        }

        chain.doFilter(request, response);

        // Periodic cleanup: remove stale entries every 1000 requests
        if (count == 1 && counters.size() > 1000) {
            counters.entrySet().removeIf(e -> e.getValue().minute < currentMinute);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.startsWith("/cashier/");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        final long minute;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long minute) {
            this.minute = minute;
        }
    }
}
