package com.nexusflow.api.controller;

import com.nexusflow.application.OpsQueryApplicationService;
import com.nexusflow.application.dto.OpsFiatRampPageResponse;
import com.nexusflow.application.dto.OpsOrderPageResponse;
import com.nexusflow.application.dto.OpsPaymentPageResponse;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.domain.fiat.FiatRampStatus;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.permission.client.CheckPermission;
import com.nexusflow.permission.client.PermissionCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paged list queries for the ops console (orders, execution payments, fiat ramp orders).
 */
@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
public class OpsQueryController {

    private final OpsQueryApplicationService queryService;

    @GetMapping("/orders")
    @CheckPermission(value = PermissionCodes.OpsDashboard.READ, scopeType = "SYSTEM")
    public ApiResponse<OpsOrderPageResponse> orders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "merchantId", required = false) String merchantId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        OrderStatus orderStatus = parseStatus(status, OrderStatus.class, "order status");
        return ApiResponse.ok(queryService.searchOrders(orderStatus, blankToNull(merchantId), page, size));
    }

    @GetMapping("/payments")
    @CheckPermission(value = PermissionCodes.OpsDashboard.READ, scopeType = "SYSTEM")
    public ApiResponse<OpsPaymentPageResponse> payments(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PaymentStatus paymentStatus = parseStatus(status, PaymentStatus.class, "payment status");
        return ApiResponse.ok(queryService.searchPayments(paymentStatus, page, size));
    }

    @GetMapping("/fiat-ramp-orders")
    @CheckPermission(value = PermissionCodes.OpsDashboard.READ, scopeType = "SYSTEM")
    public ApiResponse<OpsFiatRampPageResponse> fiatRampOrders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "merchantId", required = false) String merchantId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        FiatRampStatus rampStatus = parseStatus(status, FiatRampStatus.class, "fiat ramp status");
        return ApiResponse.ok(queryService.searchFiatRampOrders(rampStatus, blankToNull(merchantId), page, size));
    }

    private <E extends Enum<E>> E parseStatus(String raw, Class<E> type, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new com.nexusflow.common.NexusFlowException(
                    com.nexusflow.common.ErrorCode.INVALID_REQUEST,
                    "Invalid " + label + ": " + raw);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
