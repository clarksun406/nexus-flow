package com.nexusflow.domain.channel;

import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;

@Value @Builder
public class ChannelRefund {
    String channelRefundId;
    String status;
    BigDecimal refundAmount;
    String txHash;
}