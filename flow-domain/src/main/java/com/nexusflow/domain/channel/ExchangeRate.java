package com.nexusflow.domain.channel;

import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;
import java.time.Instant;

@Value @Builder
public class ExchangeRate {
    String token;
    String network;
    BigDecimal price;
    String quoteCurrency;
    Instant timestamp;
}