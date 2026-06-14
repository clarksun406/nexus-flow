package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value @Builder
@Jacksonized
public class CashierSubmitRequest {
    @NotBlank String paymentId;
    @NotBlank String token;
    @NotBlank String network;
    String channelId;
}
