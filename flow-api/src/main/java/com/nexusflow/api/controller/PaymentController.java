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
     * Idempotent via orderId.
     */
    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentCommand command) {
        PaymentResponse response = paymentService.createPayment(command);
        return ApiResponse.ok(response);
    }

    /**
     * Query payment by ID.
     */
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ApiResponse.ok(response);
    }

    /**
     * Webhook: mark payment as confirmed (from NexusPay-Core or manual).
     */
    @PostMapping("/{paymentId}/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(@PathVariable String paymentId,
                                                        @RequestParam(defaultValue = "12") int confirmations) {
        paymentService.confirmPayment(paymentId, confirmations);
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ApiResponse.ok(response);
    }

    /**
     * Webhook: mark payment as failed.
     */
    @PostMapping("/{paymentId}/fail")
    public ApiResponse<PaymentResponse> failPayment(@PathVariable String paymentId,
                                                     @RequestParam(defaultValue = "Unknown") String reason) {
        paymentService.failPayment(paymentId, reason);
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ApiResponse.ok(response);
    }
}