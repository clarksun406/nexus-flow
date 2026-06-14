package com.nexusflow.domain.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class PaymentFlow {

    private String flowNo;
    private String paymentId;
    private String channelId;
    private String token;
    private String network;
    private BigDecimal cryptoAmount;
    private BigDecimal fiatAmount;
    private String fiatCurrency;
    private BigDecimal exchangeRate;
    private String payAddress;
    private String memo;
    private String payerAddress;
    private FlowStatus status;
    private String txHash;
    private BigDecimal paidAmount;
    private Instant createTime;
    private Instant updateTime;

    @Builder
    public PaymentFlow(String flowNo, String paymentId, String channelId,
                       String token, String network, BigDecimal cryptoAmount,
                       BigDecimal fiatAmount, String fiatCurrency, BigDecimal exchangeRate,
                       String payAddress, String memo, String payerAddress) {
        this.flowNo = flowNo;
        this.paymentId = paymentId;
        this.channelId = channelId;
        this.token = token;
        this.network = network;
        this.cryptoAmount = cryptoAmount;
        this.fiatAmount = fiatAmount;
        this.fiatCurrency = fiatCurrency;
        this.exchangeRate = exchangeRate;
        this.payAddress = payAddress;
        this.memo = memo;
        this.payerAddress = payerAddress;
        this.status = FlowStatus.INIT;
        this.createTime = Instant.now();
        this.updateTime = Instant.now();
    }

    public void markWaiting() { this.status = status.requireTransitionTo(FlowStatus.WAITING); touch(); }
    public void markConfirmed(String txHash, BigDecimal paid) {
        this.txHash = txHash; this.paidAmount = paid;
        this.status = status.requireTransitionTo(FlowStatus.CONFIRMED); touch();
    }
    public void markCancelled() { this.status = status.requireTransitionTo(FlowStatus.CANCELLED); touch(); }
    public void markFailed() { this.status = status.requireTransitionTo(FlowStatus.FAILED); touch(); }
    /**
     * Full-args builder for reconstituting a PaymentFlow from persistence.
     */
    @Builder(builderMethodName = "reconstitute", builderClassName = "PaymentFlowReconstituteBuilder")
    private PaymentFlow(String flowNo, String paymentId, String channelId,
                        String token, String network, BigDecimal cryptoAmount,
                        BigDecimal fiatAmount, String fiatCurrency, BigDecimal exchangeRate,
                        String payAddress, String memo, String payerAddress,
                        FlowStatus status, String txHash, BigDecimal paidAmount,
                        Instant createTime, Instant updateTime) {
        this.flowNo = flowNo;
        this.paymentId = paymentId;
        this.channelId = channelId;
        this.token = token;
        this.network = network;
        this.cryptoAmount = cryptoAmount;
        this.fiatAmount = fiatAmount;
        this.fiatCurrency = fiatCurrency;
        this.exchangeRate = exchangeRate;
        this.payAddress = payAddress;
        this.memo = memo;
        this.payerAddress = payerAddress;
        this.status = status;
        this.txHash = txHash;
        this.paidAmount = paidAmount;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    private void touch() { this.updateTime = Instant.now(); }
}
