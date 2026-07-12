package com.nexusflow.api.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class ActiveMerchantRequest {
    @NotBlank
    String merchantId;
}
