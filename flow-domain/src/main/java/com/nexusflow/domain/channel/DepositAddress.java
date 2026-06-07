package com.nexusflow.domain.channel;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class DepositAddress {
    String address;
    String memo;
    String channelOrderId;
    int requiredConfirmations;
}