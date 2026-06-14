package com.nexusflow.domain.gas;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class GasTopUpResult {
    String requestId;
    Chain chain;
    String network;
    String walletAddress;
    String nativeCurrency;
    BigDecimal amount;
    String status;
    String txHash;
    Instant requestedAt;
}
