package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class FiatRampCallbackRequestDto {
    @NotBlank String providerOrderId;
    @NotBlank String status;
    String fiatTransferId;
    String cryptoTxHash;
    String failureReason;
}
