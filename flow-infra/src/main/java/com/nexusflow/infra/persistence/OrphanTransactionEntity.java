package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "orphan_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_orphan_transaction_chain_tx", columnNames = {"chain", "tx_hash"}))
public class OrphanTransactionEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 20, nullable = false)
    private String chain;

    @Column(name = "tx_hash", length = 128, nullable = false)
    private String txHash;

    @Column(name = "to_address", length = 256, nullable = false)
    private String toAddress;

    @Column(length = 80, nullable = false)
    private String amount;

    @Column(length = 32, nullable = false)
    private String currency;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "seen_count", nullable = false)
    private Integer seenCount;

    @Column(name = "resolved_payment_id", length = 64)
    private String resolvedPaymentId;

    @Version
    private Long version;
}
