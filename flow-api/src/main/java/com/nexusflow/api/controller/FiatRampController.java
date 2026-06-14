package com.nexusflow.api.controller;

import com.nexusflow.application.FiatRampApplicationService;
import com.nexusflow.application.dto.FiatRampCreateOrderRequestDto;
import com.nexusflow.application.dto.FiatRampOrderResponseDto;
import com.nexusflow.application.dto.FiatRampQuoteRequestDto;
import com.nexusflow.application.dto.FiatRampQuoteResponseDto;
import com.nexusflow.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fiat/ramp")
@RequiredArgsConstructor
public class FiatRampController {

    private final FiatRampApplicationService service;

    @PostMapping("/quote")
    public ApiResponse<FiatRampQuoteResponseDto> quote(@Valid @RequestBody FiatRampQuoteRequestDto request) {
        return ApiResponse.ok(service.quote(request));
    }

    @PostMapping("/orders")
    public ApiResponse<FiatRampOrderResponseDto> createOrder(
            @Valid @RequestBody FiatRampCreateOrderRequestDto request) {
        return ApiResponse.ok(service.createOrder(request));
    }

    @GetMapping("/orders/{rampOrderId}")
    public ApiResponse<FiatRampOrderResponseDto> getOrder(@PathVariable("rampOrderId") String rampOrderId) {
        return ApiResponse.ok(service.getOrder(rampOrderId));
    }
}
