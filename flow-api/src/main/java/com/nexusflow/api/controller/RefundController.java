package com.nexusflow.api.controller;

import com.nexusflow.application.PaymentOrchestrator;
import com.nexusflow.application.dto.*;
import com.nexusflow.common.ApiResponse;
import com.nexusflow.permission.client.CheckPermission;
import com.nexusflow.permission.client.PermissionCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
public class RefundController {

    private final PaymentOrchestrator orchestrator;

    @PostMapping("/order")
    @CheckPermission(PermissionCodes.Refund.CREATE)
    public ApiResponse<RefundResponseDto> refund(HttpServletRequest request,
                                                 @Valid @RequestBody RefundRequestDto req) {
        MerchantRequestGuard.requireMatchingMerchant(request, req.getMerchantId());
        return ApiResponse.ok(orchestrator.refund(req));
    }
}
