package com.nexusflow.permission.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ServiceTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final boolean authEnabled;
    private final String serviceToken;

    public ServiceTokenFilter(boolean authEnabled, String serviceToken) {
        this.authEnabled = authEnabled;
        this.serviceToken = serviceToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!authEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        if (serviceToken == null || serviceToken.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Permission service token is not configured");
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!matchesConfiguredToken(authorization)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid permission service token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matchesConfiguredToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        byte[] expected = (BEARER_PREFIX + serviceToken).getBytes(StandardCharsets.UTF_8);
        byte[] actual = authorization.getBytes(StandardCharsets.UTF_8);
        return actual.length == expected.length && MessageDigest.isEqual(actual, expected);
    }
}
