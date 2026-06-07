package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

@Value @Builder
public class RefundRequestDto {
    @NotBlank String merchantId;
    @NotBlank String merchantOrderNo;
    @NotBlank String refundOrderNo;
    @NotBlank @Positive String refundAmountFiat;
    String notifyUrl;
}