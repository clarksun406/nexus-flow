package com.nexusflow.domain.channel;

import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;

@Value @Builder
public class CurrencyConfig {
    String token;
    String network;
    String contractAddress;
    int decimals;
    BigDecimal minDeposit;
    int requiredConfirmations;
    boolean enabled;
}