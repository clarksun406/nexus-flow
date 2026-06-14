package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "fiat_ramp_orders")
public class FiatRampOrderEntity {

    @Id
    @Column(name = "ramp_order_id", length = 64)
    private String rampOrderId;

    @Column(name = "merchant_id", length = 128, nullable = false)
    private String merchantId;

    @Column(name = "merchant_order_no", length = 128, nullable = false)
    private String merchantOrderNo;

    @Column(name = "payment_id", length = 64)
    private String paymentId;

    @Column(length = 20, nullable = false)
    private String direction;

    @Column(name = "provider_id", length = 64, nullable = false)
    private String providerId;

    @Column(name = "provider_order_id", length = 128)
    private String providerOrderId;

    @Column(name = "quote_id", length = 128)
    private String quoteId;

    @Column(name = "fiat_amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal fiatAmount;

    @Column(name = "fiat_currency", length = 16, nullable = false)
    private String fiatCurrency;

    @Column(name = "crypto_amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal cryptoAmount;

    @Column(length = 32, nullable = false)
    private String token;

    @Column(length = 32, nullable = false)
    private String network;

    @Column(name = "exchange_rate", precision = 36, scale = 18, nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "fee_amount_fiat", precision = 36, scale = 18)
    private BigDecimal feeAmountFiat;

    @Column(name = "wallet_address", length = 256)
    private String walletAddress;

    @Column(name = "checkout_url", length = 1024)
    private String checkoutUrl;

    @Column(name = "fiat_transfer_id", length = 128)
    private String fiatTransferId;

    @Column(name = "crypto_tx_hash", length = 128)
    private String cryptoTxHash;

    @Column(name = "notify_url", length = 512)
    private String notifyUrl;

    @Column(name = "return_url", length = 512)
    private String returnUrl;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(name = "expire_time")
    private Instant expireTime;

    @Column(name = "complete_time")
    private Instant completeTime;

    @Column(name = "create_time")
    private Instant createTime;

    @Column(name = "update_time")
    private Instant updateTime;

    @Version
    private Long version;
}
