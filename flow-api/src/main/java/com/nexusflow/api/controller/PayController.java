package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.application.dto.*;
import com.nexusflow.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PaymentOrchestrator orchestrator;

    @PostMapping("/order")
    public ApiResponse<OrderResponse> createOrder(HttpServletRequest request,
                                                  @Valid @RequestBody CreateOrderRequest req) {
        MerchantRequestGuard.requireMatchingMerchant(request, req.getMerchantId());
        return ApiResponse.ok(orchestrator.createOrder(req));
    }

    @GetMapping("/order/{paymentId}")
    public ApiResponse<OrderResponse> getOrder(HttpServletRequest request,
                                               @PathVariable("paymentId") String paymentId) {
        OrderResponse order = orchestrator.getOrder(paymentId);
        MerchantRequestGuard.requireMatchingMerchant(request, order.getMerchantId());
        return ApiResponse.ok(order);
    }
}
