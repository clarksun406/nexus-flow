package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.application.dto.*;
import com.nexusflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cashier")
@RequiredArgsConstructor
public class CashierController {

    private final PaymentOrchestrator orchestrator;

    @GetMapping("/order/status")
    public ApiResponse<CashierStatusResponse> getStatus(@RequestParam String paymentId) {
        return ApiResponse.ok(orchestrator.getCashierStatus(paymentId));
    }

    @PostMapping("/pay/submit")
    public ApiResponse<CashierSubmitResponse> submit(@Valid @RequestBody CashierSubmitRequest req) {
        return ApiResponse.ok(orchestrator.submitPayment(req));
    }
}