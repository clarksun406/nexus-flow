package com.nexusflow.domain.shared;

import lombok.Value;

import java.math.BigDecimal;

/**
 * Currency + amount value object.
 */
@Value(staticConstructor = "of")
public class Money {

    String currency;  // e.g. "USDT_TRC20", "ETH"
    BigDecimal amount;

    public boolean isPositive() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}