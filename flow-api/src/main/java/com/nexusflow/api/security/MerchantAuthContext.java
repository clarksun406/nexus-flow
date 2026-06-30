package com.nexusflow.api.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public final class MerchantAuthContext {

    public static final String MERCHANT_ID_ATTRIBUTE = "nexusflow.merchantId";
    public static final String MERCHANT_CODE_ATTRIBUTE = "nexusflow.merchantCode";
    public static final String AUTH_SOURCE_ATTRIBUTE = "nexusflow.authSource";

    private MerchantAuthContext() {
    }

    public static Optional<String> merchantId(HttpServletRequest request) {
        Object value = request.getAttribute(MERCHANT_ID_ATTRIBUTE);
        if (value instanceof String merchantId && !merchantId.isBlank()) {
            return Optional.of(merchantId);
        }
        return Optional.empty();
    }

    public static Optional<String> merchantCode(HttpServletRequest request) {
        Object value = request.getAttribute(MERCHANT_CODE_ATTRIBUTE);
        if (value instanceof String merchantCode && !merchantCode.isBlank()) {
            return Optional.of(merchantCode);
        }
        return Optional.empty();
    }

    public static Optional<String> authSource(HttpServletRequest request) {
        Object value = request.getAttribute(AUTH_SOURCE_ATTRIBUTE);
        if (value instanceof String authSource && !authSource.isBlank()) {
            return Optional.of(authSource);
        }
        return Optional.empty();
    }
}
