package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.application.dto.*;
import com.nexusflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PaymentOrchestrator orchestrator;

    @PostMapping("/order")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        return ApiResponse.ok(orchestrator.createOrder(req));
    }

    @GetMapping("/order/{paymentId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable String paymentId) {
        return ApiResponse.ok(orchestrator.getOrder(paymentId));
    }
}