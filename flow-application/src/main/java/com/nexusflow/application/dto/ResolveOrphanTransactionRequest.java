package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveOrphanTransactionRequest {

    @NotBlank
    private String paymentId;
}
