package com.nexusflow.domain.event;

import com.nexusflow.domain.shared.Chain;
import lombok.Getter;

@Getter
public class OrphanTransactionDetectedEvent extends DomainEvent {

    private final Chain chain;
    private final String txHash;
    private final String toAddress;
    private final String amount;
    private final String currency;
    private final Long blockNumber;
    private final Integer seenCount;

    public OrphanTransactionDetectedEvent(Chain chain, String txHash, String toAddress,
                                          String amount, String currency, Long blockNumber,
                                          Integer seenCount) {
        this.chain = chain;
        this.txHash = txHash;
        this.toAddress = toAddress;
        this.amount = amount;
        this.currency = currency;
        this.blockNumber = blockNumber;
        this.seenCount = seenCount;
    }

    @Override
    public String eventType() {
        return "crypto.orphan.detected";
    }
}
