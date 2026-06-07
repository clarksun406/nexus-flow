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
            @PathVariable String channelId,
            @RequestBody Map<String, Object> body) {
        log.info("Payment callback from {}: {}", channelId, body);
        String channelOrderId = (String) body.getOrDefault("reference_order_no", body.get("order_id"));
        String txHash = (String) body.get("tx_hash");
        String paidCrypto = String.valueOf(body.getOrDefault("cumulative_amount", body.get("amount")));
        String paidFiat = body.containsKey("amount_fiat") ? String.valueOf(body.get("amount_fiat")) : null;
        String eventId = (String) body.getOrDefault("event_id", txHash);

        orchestrator.handlePaymentCallback(channelId, channelOrderId, txHash, paidCrypto, paidFiat, eventId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{channelId}/refund")
    public ApiResponse<Void> handleRefundCallback(
            @PathVariable String channelId,
            @RequestBody Map<String, Object> body) {
        log.info("Refund callback from {}: {}", channelId, body);
        String channelRefundId = (String) body.get("refund_id");
        String status = (String) body.get("status");
        String txHash = (String) body.get("tx_hash");

        orchestrator.handleRefundCallback(channelRefundId, status, txHash);
        return ApiResponse.ok(null);
    }
}