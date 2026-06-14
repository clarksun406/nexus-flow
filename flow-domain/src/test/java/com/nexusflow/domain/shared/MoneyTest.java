package com.nexusflow.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void positiveAmountIsPositive() {
        assertThat(Money.of("USDT_TRC20", new BigDecimal("0.000001")).isPositive()).isTrue();
    }

    @Test
    void zeroNegativeAndNullAmountsAreNotPositive() {
        assertThat(Money.of("USDT_TRC20", BigDecimal.ZERO).isPositive()).isFalse();
        assertThat(Money.of("USDT_TRC20", new BigDecimal("-1")).isPositive()).isFalse();
        assertThat(Money.of("USDT_TRC20", null).isPositive()).isFalse();
    }

    @Test
    void toStringUsesPlainDecimalFormat() {
        Money money = Money.of("BTC", new BigDecimal("1E-8"));

        assertThat(money.toString()).isEqualTo("0.00000001 BTC");
    }
}
