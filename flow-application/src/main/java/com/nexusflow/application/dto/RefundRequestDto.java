package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

@Value @Builder
public class RefundRequestDto {
    @NotBlank String merchantId;
    @NotBlank String merchantOrderNo;
    @NotBlank String refundOrderNo;
    @NotBlank @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?$", message = "refundAmountFiat must be a positive decimal") String refundAmountFiat;
    String toAddress;
    String notifyUrl;
}
