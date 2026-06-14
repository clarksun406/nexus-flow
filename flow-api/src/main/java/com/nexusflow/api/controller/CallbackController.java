package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Unified callback endpoint for all channels.
 * Each channel posts to /callback/{channelId}/payment or /refund.
 */
@Slf4j
@RestController
@RequestMapping("/callback")
@RequiredArgsConstructor
public class CallbackController {

    private final PaymentOrchestrator orchestrator;

    @PostMapping("/{channelId}/payment")
    public ApiResponse<Void> handlePaymentCallback(
            @PathVariable("channelId") String channelId,
            @RequestBody Map<String, Object> body) {
        log.debug("Payment callback from {}: {}", channelId, body);
        String channelOrderId = safeString(body, "reference_order_no", safeString(body, "order_id", null));
        String txHash = safeString(body, "tx_hash", null);
        String paidCrypto = String.valueOf(body.getOrDefault("cumulative_amount", body.get("amount")));
        String paidFiat = body.containsKey("amount_fiat") ? String.valueOf(body.get("amount_fiat")) : null;
        String eventId = safeString(body, "event_id", txHash);

        log.info("Payment callback: channel={}, channelOrderId={}, txHash={}", channelId, channelOrderId, txHash);
        orchestrator.handlePaymentCallback(channelId, channelOrderId, txHash, paidCrypto, paidFiat, eventId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{channelId}/refund")
    public ApiResponse<Void> handleRefundCallback(
            @PathVariable("channelId") String channelId,
            @RequestBody Map<String, Object> body) {
        log.debug("Refund callback from {}: {}", channelId, body);
        String channelRefundId = safeString(body, "refund_id", null);
        String status = safeString(body, "status", null);
        String txHash = safeString(body, "tx_hash", null);

        log.info("Refund callback: channel={}, channelRefundId={}, status={}", channelId, channelRefundId, status);
        orchestrator.handleRefundCallback(channelRefundId, status, txHash);
        return ApiResponse.ok(null);
    }

    private static String safeString(Map<String, Object> body, String key, String fallback) {
        Object val = body.get(key);
        return val != null ? String.valueOf(val) : fallback;
    }
}
