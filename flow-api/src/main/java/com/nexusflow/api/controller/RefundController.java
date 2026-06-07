package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.application.dto.*;
import com.nexusflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
public class RefundController {

    private final PaymentOrchestrator orchestrator;

    @PostMapping("/order")
    public ApiResponse<RefundResponseDto> refund(@Valid @RequestBody RefundRequestDto req) {
        return ApiResponse.ok(orchestrator.refund(req));
    }
}