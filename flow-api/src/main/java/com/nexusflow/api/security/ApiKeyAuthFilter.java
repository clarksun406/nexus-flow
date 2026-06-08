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

/**
 * API Key authentication filter for merchant-facing endpoints.
 *
 * Validates the {@code X-API-Key} header against a configured key.
 * Endpoints that have their own auth mechanism (callbacks use HMAC,
 * cashier is end-user facing, actuator is ops) are excluded.
 */
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    private final String configuredKey;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(String configuredKey, ObjectMapper objectMapper) {
        this.configuredKey = configuredKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey == null || !apiKey.equals(configuredKey)) {
            log.warn("Unauthorized request: missing or invalid API key from {}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.fail(ErrorCode.UNAUTHORIZED, "Missing or invalid API key")));
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Callback endpoints use HMAC verification instead
        if (path.startsWith("/callback/")) return true;
        // Cashier endpoints are end-user facing (no merchant API key)
        if (path.startsWith("/cashier/")) return true;
        // Actuator endpoints have their own security
        if (path.startsWith("/actuator/")) return true;
        return false;
    }
}
