package com.nexusflow.domain.gas;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class GasTopUpRequest {
    String requestId;
    Chain chain;
    String network;
    String walletAddress;
    String nativeCurrency;
    BigDecimal amount;
    String reason;
}
