package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class RefundResponseDto {
    String refundOrderNo;
    String paymentId;
    String channelRefundId;
    String status;
    String refundAmountFiat;
    String refundAmountCrypto;
    String exchangeRate;
    String token;
    String network;
    String toAddress;
    String txHash;
    Long createTime;
    Long confirmTime;
}