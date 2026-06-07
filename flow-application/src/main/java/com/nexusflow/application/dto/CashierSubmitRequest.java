package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

@Value @Builder
public class CashierSubmitRequest {
    @NotBlank String paymentId;
    @NotBlank String token;
    @NotBlank String network;
    String channelId;
}