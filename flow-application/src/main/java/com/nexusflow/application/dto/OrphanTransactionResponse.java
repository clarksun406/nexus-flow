package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrphanTransactionResponse {

    private String id;
    private String chain;
    private String txHash;
    private String toAddress;
    private String amount;
    private String currency;
    private Long blockNumber;
    private String status;
    private Long firstSeenAt;
    private Long lastSeenAt;
    private Integer seenCount;
    private String resolvedPaymentId;
}
