package com.nexusflow.domain.blockchain;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
public class OrphanTransaction {

    private String id;
    private Chain chain;
    private String txHash;
    private String toAddress;
    private String amount;
    private String currency;
    private Long blockNumber;
    private OrphanTransactionStatus status;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private Integer seenCount;
    private String resolvedPaymentId;

    @Builder
    public OrphanTransaction(String id, Chain chain, String txHash, String toAddress,
                             String amount, String currency, Long blockNumber) {
        this.id = id;
        this.chain = chain;
        this.txHash = txHash;
        this.toAddress = toAddress;
        this.amount = amount;
        this.currency = currency;
        this.blockNumber = blockNumber;
        this.status = OrphanTransactionStatus.UNMATCHED;
        this.firstSeenAt = Instant.now();
        this.lastSeenAt = this.firstSeenAt;
        this.seenCount = 1;
    }

    @Builder(builderMethodName = "reconstitute", builderClassName = "OrphanTransactionReconstituteBuilder")
    private OrphanTransaction(String id, Chain chain, String txHash, String toAddress,
                              String amount, String currency, Long blockNumber,
                              OrphanTransactionStatus status, Instant firstSeenAt,
                              Instant lastSeenAt, Integer seenCount, String resolvedPaymentId) {
        this.id = id;
        this.chain = chain;
        this.txHash = txHash;
        this.toAddress = toAddress;
        this.amount = amount;
        this.currency = currency;
        this.blockNumber = blockNumber;
        this.status = status;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.seenCount = seenCount;
        this.resolvedPaymentId = resolvedPaymentId;
    }

    public void markSeenAgain(Long latestBlockNumber) {
        if (latestBlockNumber != null) {
            this.blockNumber = latestBlockNumber;
        }
        this.lastSeenAt = Instant.now();
        this.seenCount = this.seenCount == null ? 1 : this.seenCount + 1;
    }

    public void resolve(String paymentId) {
        this.status = OrphanTransactionStatus.RESOLVED;
        this.resolvedPaymentId = paymentId;
        this.lastSeenAt = Instant.now();
    }

    public void ignore() {
        this.status = OrphanTransactionStatus.IGNORED;
        this.lastSeenAt = Instant.now();
    }
}
