package com.nexusflow.domain;

import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampStatus;
import com.nexusflow.domain.order.FlowStatus;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentFlow;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.refund.RefundOrder;
import com.nexusflow.domain.refund.RefundStatus;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReconstituteBuilderTest {

    @Test
    void paymentOrderReconstituteRestoresPersistentFields() {
        Instant created = Instant.parse("2026-06-13T00:00:00Z");
        PaymentOrder order = PaymentOrder.reconstitute()
                .paymentId("pay-1")
                .merchantId("merchant-1")
                .merchantOrderNo("order-1")
                .amountFiat(new BigDecimal("100.00"))
                .currencyFiat("USD")
                .channelOrderId("channel-order-1")
                .status(OrderStatus.CONFIRMED)
                .txHash("tx-1")
                .paidAmountCrypto(new BigDecimal("100.00"))
                .createTime(created)
                .updateTime(created)
                .build();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getChannelOrderId()).isEqualTo("channel-order-1");
        assertThat(order.getTxHash()).isEqualTo("tx-1");
        assertThat(order.getPaidAmountCrypto()).isEqualByComparingTo("100.00");
        assertThat(order.getCreateTime()).isEqualTo(created);
    }

    @Test
    void paymentFlowReconstituteRestoresPersistentFields() {
        Instant created = Instant.parse("2026-06-13T00:00:00Z");
        PaymentFlow flow = PaymentFlow.reconstitute()
                .flowNo("flow-1")
                .paymentId("pay-1")
                .status(FlowStatus.CONFIRMED)
                .txHash("tx-1")
                .paidAmount(new BigDecimal("100.00"))
                .createTime(created)
                .updateTime(created)
                .build();

        assertThat(flow.getStatus()).isEqualTo(FlowStatus.CONFIRMED);
        assertThat(flow.getTxHash()).isEqualTo("tx-1");
        assertThat(flow.getPaidAmount()).isEqualByComparingTo("100.00");
        assertThat(flow.getCreateTime()).isEqualTo(created);
    }

    @Test
    void refundOrderReconstituteRestoresPersistentFields() {
        Instant created = Instant.parse("2026-06-13T00:00:00Z");
        RefundOrder refund = RefundOrder.reconstitute()
                .refundOrderNo("refund-1")
                .paymentId("pay-1")
                .channelRefundId("channel-refund-1")
                .status(RefundStatus.SUCCESS)
                .txHash("tx-1")
                .createTime(created)
                .confirmTime(created)
                .updateTime(created)
                .build();

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(refund.getChannelRefundId()).isEqualTo("channel-refund-1");
        assertThat(refund.getTxHash()).isEqualTo("tx-1");
        assertThat(refund.getConfirmTime()).isEqualTo(created);
    }

    @Test
    void walletReconstituteRestoresMpcWalletId() {
        Instant created = Instant.parse("2026-06-13T00:00:00Z");
        Wallet wallet = Wallet.reconstitute()
                .id("wallet-1")
                .name("MPC ETH hot")
                .chain(Chain.ETH)
                .type(WalletType.HOT)
                .address("0xabc")
                .encryptedPrivateKey("ciphertext")
                .kmsKeyId("kms-key-1")
                .mpcWalletId("mpc-wallet-1")
                .active(true)
                .createdAt(created)
                .updatedAt(created)
                .build();

        assertThat(wallet.getMpcWalletId()).isEqualTo("mpc-wallet-1");
        assertThat(wallet.getChain()).isEqualTo(Chain.ETH);
        assertThat(wallet.getCreatedAt()).isEqualTo(created);
    }

    @Test
    void fiatRampOrderReconstituteRestoresConversionTrackingFields() {
        Instant created = Instant.parse("2026-06-13T00:00:00Z");
        FiatRampOrder order = FiatRampOrder.reconstitute()
                .rampOrderId("ramp-order-1")
                .merchantId("merchant-1")
                .merchantOrderNo("merchant-order-1")
                .paymentId("pay-1")
                .direction(FiatRampDirection.ON_RAMP)
                .providerId("MOONPAY")
                .providerOrderId("provider-order-1")
                .quoteId("quote-1")
                .fiatAmount(new BigDecimal("100.00"))
                .fiatCurrency("USD")
                .cryptoAmount(new BigDecimal("100.000000"))
                .token("USDT")
                .network("TRC20")
                .exchangeRate(new BigDecimal("1.00"))
                .feeAmountFiat(new BigDecimal("2.50"))
                .walletAddress("TDEST")
                .checkoutUrl("https://ramp.example/checkout/1")
                .fiatTransferId("fiat-transfer-1")
                .cryptoTxHash("0xtx")
                .status(FiatRampStatus.COMPLETED)
                .completeTime(created)
                .createTime(created)
                .updateTime(created)
                .build();

        assertThat(order.getStatus()).isEqualTo(FiatRampStatus.COMPLETED);
        assertThat(order.getDirection()).isEqualTo(FiatRampDirection.ON_RAMP);
        assertThat(order.getProviderOrderId()).isEqualTo("provider-order-1");
        assertThat(order.getFiatTransferId()).isEqualTo("fiat-transfer-1");
        assertThat(order.getCryptoTxHash()).isEqualTo("0xtx");
        assertThat(order.getCompleteTime()).isEqualTo(created);
    }
}
