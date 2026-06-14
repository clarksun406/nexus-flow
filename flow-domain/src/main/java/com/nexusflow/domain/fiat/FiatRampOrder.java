package com.nexusflow.domain.fiat;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class FiatRampOrder {

    private String rampOrderId;
    private String merchantId;
    private String merchantOrderNo;
    private String paymentId;
    private FiatRampDirection direction;
    private String providerId;
    private String providerOrderId;
    private String quoteId;
    private BigDecimal fiatAmount;
    private String fiatCurrency;
    private BigDecimal cryptoAmount;
    private String token;
    private String network;
    private BigDecimal exchangeRate;
    private BigDecimal feeAmountFiat;
    private String walletAddress;
    private String checkoutUrl;
    private String fiatTransferId;
    private String cryptoTxHash;
    private String notifyUrl;
    private String returnUrl;
    private String failureReason;
    private FiatRampStatus status;
    private Instant expireTime;
    private Instant completeTime;
    private Instant createTime;
    private Instant updateTime;

    @Builder
    public FiatRampOrder(String rampOrderId, String merchantId, String merchantOrderNo,
                         String paymentId, FiatRampDirection direction, String providerId,
                         String quoteId, BigDecimal fiatAmount, String fiatCurrency,
                         BigDecimal cryptoAmount, String token, String network,
                         BigDecimal exchangeRate, BigDecimal feeAmountFiat,
                         String walletAddress, String notifyUrl, String returnUrl,
                         Instant expireTime) {
        this.rampOrderId = rampOrderId;
        this.merchantId = merchantId;
        this.merchantOrderNo = merchantOrderNo;
        this.paymentId = paymentId;
        this.direction = direction;
        this.providerId = providerId;
        this.quoteId = quoteId;
        this.fiatAmount = fiatAmount;
        this.fiatCurrency = fiatCurrency;
        this.cryptoAmount = cryptoAmount;
        this.token = token;
        this.network = network;
        this.exchangeRate = exchangeRate;
        this.feeAmountFiat = feeAmountFiat;
        this.walletAddress = walletAddress;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
        this.expireTime = expireTime;
        this.status = FiatRampStatus.CREATED;
        this.createTime = Instant.now();
        this.updateTime = Instant.now();
    }

    public void bindProviderOrder(String providerOrderId, String checkoutUrl) {
        this.providerOrderId = providerOrderId;
        this.checkoutUrl = checkoutUrl;
        this.status = status.requireTransitionTo(FiatRampStatus.PENDING_PAYMENT);
        touch();
    }

    public void markProcessing(String fiatTransferId, String cryptoTxHash) {
        updateSettlementReferences(fiatTransferId, cryptoTxHash);
        this.status = status.requireTransitionTo(FiatRampStatus.PROCESSING);
        touch();
    }

    public void markCompleted(String fiatTransferId, String cryptoTxHash) {
        updateSettlementReferences(fiatTransferId, cryptoTxHash);
        this.status = status.requireTransitionTo(FiatRampStatus.COMPLETED);
        this.completeTime = Instant.now();
        touch();
    }

    public void markFailed(String failureReason) {
        this.failureReason = failureReason;
        this.status = status.requireTransitionTo(FiatRampStatus.FAILED);
        touch();
    }

    public void markExpired() {
        this.status = status.requireTransitionTo(FiatRampStatus.EXPIRED);
        touch();
    }

    public void markCancelled() {
        this.status = status.requireTransitionTo(FiatRampStatus.CANCELLED);
        touch();
    }

    @Builder(builderMethodName = "reconstitute", builderClassName = "FiatRampOrderReconstituteBuilder")
    private FiatRampOrder(String rampOrderId, String merchantId, String merchantOrderNo,
                          String paymentId, FiatRampDirection direction, String providerId,
                          String providerOrderId, String quoteId, BigDecimal fiatAmount,
                          String fiatCurrency, BigDecimal cryptoAmount, String token,
                          String network, BigDecimal exchangeRate, BigDecimal feeAmountFiat,
                          String walletAddress, String checkoutUrl, String fiatTransferId,
                          String cryptoTxHash, String notifyUrl, String returnUrl,
                          String failureReason, FiatRampStatus status, Instant expireTime,
                          Instant completeTime, Instant createTime, Instant updateTime) {
        this.rampOrderId = rampOrderId;
        this.merchantId = merchantId;
        this.merchantOrderNo = merchantOrderNo;
        this.paymentId = paymentId;
        this.direction = direction;
        this.providerId = providerId;
        this.providerOrderId = providerOrderId;
        this.quoteId = quoteId;
        this.fiatAmount = fiatAmount;
        this.fiatCurrency = fiatCurrency;
        this.cryptoAmount = cryptoAmount;
        this.token = token;
        this.network = network;
        this.exchangeRate = exchangeRate;
        this.feeAmountFiat = feeAmountFiat;
        this.walletAddress = walletAddress;
        this.checkoutUrl = checkoutUrl;
        this.fiatTransferId = fiatTransferId;
        this.cryptoTxHash = cryptoTxHash;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
        this.failureReason = failureReason;
        this.status = status;
        this.expireTime = expireTime;
        this.completeTime = completeTime;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    private void updateSettlementReferences(String fiatTransferId, String cryptoTxHash) {
        if (fiatTransferId != null && !fiatTransferId.isBlank()) {
            this.fiatTransferId = fiatTransferId;
        }
        if (cryptoTxHash != null && !cryptoTxHash.isBlank()) {
            this.cryptoTxHash = cryptoTxHash;
        }
    }

    private void touch() {
        this.updateTime = Instant.now();
    }
}
