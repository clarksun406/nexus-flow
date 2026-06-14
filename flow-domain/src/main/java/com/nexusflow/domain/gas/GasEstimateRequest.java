package com.nexusflow.domain.gas;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GasEstimateRequest {
    Chain chain;
    String token;
    String network;
    GasOperation operation;
    BigDecimal amount;
    String toAddress;
}
