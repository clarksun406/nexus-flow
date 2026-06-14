package com.nexusflow.domain.fiat;

import com.nexusflow.common.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiatRampOrderTest {

    @Test
    void newOnRampOrderStartsCreatedAndKeepsConversionFields() {
        FiatRampOrder order = newOrder(FiatRampDirection.ON_RAMP);

        assertThat(order.getStatus()).isEqualTo(FiatRampStatus.CREATED);
        assertThat(order.getDirection()).isEqualTo(FiatRampDirection.ON_RAMP);
        assertThat(order.getFiatAmount()).isEqualByComparingTo("100.00");
        assertThat(order.getFiatCurrency()).isEqualTo("USD");
        assertThat(order.getCryptoAmount()).isEqualByComparingTo("100.000000");
        assertThat(order.getToken()).isEqualTo("USDT");
        assertThat(order.getNetwork()).isEqualTo("TRC20");
        assertThat(order.getCreateTime()).isNotNull();
    }

    @Test
    void bindProviderOrderMovesToPendingPayment() {
        FiatRampOrder order = newOrder(FiatRampDirection.ON_RAMP);

        order.bindProviderOrder("provider-order-1", "https://ramp.example/checkout/1");

        assertThat(order.getStatus()).isEqualTo(FiatRampStatus.PENDING_PAYMENT);
        assertThat(order.getProviderOrderId()).isEqualTo("provider-order-1");
        assertThat(order.getCheckoutUrl()).isEqualTo("https://ramp.example/checkout/1");
    }

    @Test
    void markProcessingAndCompletedTrackFiatAndCryptoSettlementReferences() {
        FiatRampOrder order = newOrder(FiatRampDirection.ON_RAMP);
        order.bindProviderOrder("provider-order-1", "https://ramp.example/checkout/1");

        order.markProcessing("fiat-transfer-1", null);
        order.markCompleted(null, "0xtx");

        assertThat(order.getStatus()).isEqualTo(FiatRampStatus.COMPLETED);
        assertThat(order.getFiatTransferId()).isEqualTo("fiat-transfer-1");
        assertThat(order.getCryptoTxHash()).isEqualTo("0xtx");
        assertThat(order.getCompleteTime()).isNotNull();
        assertThat(order.getStatus().isTerminal()).isTrue();
    }

    @Test
    void offRampOrderCanCompleteFromPendingPaymentWhenProviderSkipsProcessingCallback() {
        FiatRampOrder order = newOrder(FiatRampDirection.OFF_RAMP);
        order.bindProviderOrder("provider-order-2", "https://ramp.example/offramp/2");

        order.markCompleted("fiat-settlement-2", "0xsource");

        assertThat(order.getDirection()).isEqualTo(FiatRampDirection.OFF_RAMP);
        assertThat(order.getStatus()).isEqualTo(FiatRampStatus.COMPLETED);
        assertThat(order.getFiatTransferId()).isEqualTo("fiat-settlement-2");
        assertThat(order.getCryptoTxHash()).isEqualTo("0xsource");
    }

    @Test
    void markFailedStoresReasonAndBecomesTerminal() {
        FiatRampOrder order = newOrder(FiatRampDirection.ON_RAMP);

        order.markFailed("KYC_REJECTED");

        assertThat(order.getStatus()).isEqualTo(FiatRampStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo("KYC_REJECTED");
        assertThat(order.getStatus().isTerminal()).isTrue();
    }

    @Test
    void terminalOrderCannotMoveBackToProcessing() {
        FiatRampOrder order = newOrder(FiatRampDirection.ON_RAMP);
        order.markFailed("KYC_REJECTED");

        assertThatThrownBy(() -> order.markProcessing("fiat-transfer-1", "0xtx"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    private FiatRampOrder newOrder(FiatRampDirection direction) {
        return FiatRampOrder.builder()
                .rampOrderId("ramp-order-1")
                .merchantId("merchant-1")
                .merchantOrderNo("merchant-order-1")
                .paymentId("payment-1")
                .direction(direction)
                .providerId("MOONPAY")
                .quoteId("quote-1")
                .fiatAmount(new BigDecimal("100.00"))
                .fiatCurrency("USD")
                .cryptoAmount(new BigDecimal("100.000000"))
                .token("USDT")
                .network("TRC20")
                .exchangeRate(new BigDecimal("1.00"))
                .feeAmountFiat(new BigDecimal("2.50"))
                .walletAddress("TDEST")
                .notifyUrl("https://merchant.example/ramp/webhook")
                .returnUrl("https://merchant.example/return")
                .expireTime(Instant.parse("2026-06-14T01:00:00Z"))
                .build();
    }
}
