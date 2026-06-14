package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.application.dto.CreatePaymentCommand;
import com.nexusflow.application.dto.PaymentResponse;
import com.nexusflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for crypto payment operations.
 */
@RestController
@RequestMapping("/crypto/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentService;

    /**
     * Create a new crypto payment.
     * Idempotent via Idempotency-Key / X-Idempotency-Key header, or orderId fallback.
     */
    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String xIdempotencyKey,
            @Valid @RequestBody CreatePaymentCommand command) {
        String headerKey = firstNonBlank(idempotencyKey, xIdempotencyKey);
        if (headerKey != null) {
            command = command.toBuilder().idempotencyKey(headerKey).build();
        }
        PaymentResponse response = paymentService.createPayment(command);
        return ApiResponse.ok(response);
    }

    /**
     * Query payment by ID.
     */
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable("paymentId") String paymentId) {
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ApiResponse.ok(response);
    }

    /**
     * Webhook: mark payment as confirmed (from NexusPay-Core or manual).
     */
    @PostMapping("/{paymentId}/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestParam(value = "confirmations", defaultValue = "12") int confirmations) {
        paymentService.confirmPayment(paymentId, confirmations);
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ApiResponse.ok(response);
    }

    /**
     * Webhook: mark payment as failed.
     */
    @PostMapping("/{paymentId}/fail")
    public ApiResponse<PaymentResponse> failPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestParam(value = "reason", defaultValue = "Unknown") String reason) {
        paymentService.failPayment(paymentId, reason);
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ApiResponse.ok(response);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
