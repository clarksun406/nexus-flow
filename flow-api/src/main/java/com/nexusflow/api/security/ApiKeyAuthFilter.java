package com.nexusflow.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.domain.merchant.MerchantApiKey;
import com.nexusflow.domain.merchant.MerchantCredentialRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * API key authentication filter for merchant-facing endpoints.
 */
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    private final String configuredKey;
    private final MerchantCredentialRepository merchantCredentialRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(String configuredKey, MerchantCredentialRepository merchantCredentialRepository,
                            ApiKeyHasher apiKeyHasher, ObjectMapper objectMapper) {
        this.configuredKey = configuredKey;
        this.merchantCredentialRepository = merchantCredentialRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            reject(response, request);
            return;
        }

        Optional<MerchantApiKey> merchantApiKey = merchantCredentialRepository.findActiveByKeyHash(
                apiKeyHasher.hash(apiKey), Instant.now());
        if (merchantApiKey.isPresent()) {
            MerchantApiKey credential = merchantApiKey.get();
            request.setAttribute(MerchantAuthContext.MERCHANT_ID_ATTRIBUTE, credential.getMerchantId());
            request.setAttribute(MerchantAuthContext.MERCHANT_CODE_ATTRIBUTE, credential.getMerchantCode());
            request.setAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE, "merchant-api-key");

            String path = request.getRequestURI();
            if (path.startsWith("/ops") || path.startsWith("/crypto")) {
                reject(response, request, "Merchant-scoped API keys cannot access this endpoint");
                return;
            }

            chain.doFilter(request, response);
            return;
        }

        if (configuredKey != null && !configuredKey.isBlank() && apiKey.equals(configuredKey)) {
            request.setAttribute(MerchantAuthContext.AUTH_SOURCE_ATTRIBUTE, "global-api-key");
            chain.doFilter(request, response);
            return;
        }

        reject(response, request);
    }

    private void reject(HttpServletResponse response, HttpServletRequest request) throws IOException {
        reject(response, request, "Missing or invalid API key");
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, String message) throws IOException {
        log.warn("Unauthorized request: {} from {}", message, request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorCode.UNAUTHORIZED, message)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/callback/")) return true;
        if (path.startsWith("/cashier/")) return true;
        if (path.startsWith("/actuator/")) return true;
        return false;
    }
}
