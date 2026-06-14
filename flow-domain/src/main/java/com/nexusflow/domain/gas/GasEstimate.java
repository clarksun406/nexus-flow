package com.nexusflow.domain.gas;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class GasEstimate {
    Chain chain;
    String network;
    GasOperation operation;
    long gasLimit;
    BigDecimal gasPrice;
    BigDecimal estimatedFee;
    String nativeCurrency;
    Instant estimatedAt;
}
