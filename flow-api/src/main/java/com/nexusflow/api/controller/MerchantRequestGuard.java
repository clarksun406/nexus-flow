package com.nexusflow.api.controller;

import com.nexusflow.api.security.MerchantAuthContext;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.NexusFlowException;
import jakarta.servlet.http.HttpServletRequest;

public final class MerchantRequestGuard {

    private MerchantRequestGuard() {
    }

    public static void requireMatchingMerchant(HttpServletRequest request, String requestMerchantId) {
        MerchantAuthContext.merchantId(request).ifPresent(authenticatedMerchantId -> {
            if (!authenticatedMerchantId.equals(requestMerchantId)) {
                throw new NexusFlowException(ErrorCode.UNAUTHORIZED,
                        "Request merchantId does not match authenticated merchant");
            }
        });
    }

    public static void requireGlobalAccess(HttpServletRequest request) {
        MerchantAuthContext.authSource(request).ifPresent(source -> {
            if ("merchant-api-key".equals(source)) {
                throw new NexusFlowException(ErrorCode.UNAUTHORIZED,
                        "Merchant-scoped API keys cannot access this endpoint");
            }
        });
    }
}
