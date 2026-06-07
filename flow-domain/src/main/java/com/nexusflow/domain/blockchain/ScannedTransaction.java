package com.nexusflow.domain.blockchain;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Value;

/**
 * Represents a scanned on-chain transaction.
 */
@Value
@Builder
public class ScannedTransaction {

    String txHash;
    String fromAddress;
    String toAddress;
    String amount;       // smallest unit
    String contractAddress; // token contract (null for native coin)
    long blockNumber;
    int confirmations;
    long timestamp;
}