package com.nexusflow.domain.blockchain;

import java.util.List;

/**
 * Port for blockchain interaction - scanning blocks and querying transactions.
 * Implemented by chain-specific adapters in flow-infra.
 */
public interface BlockchainAdapter {

    /**
     * Scan new blocks since lastScannedBlock and return relevant transactions.
     */
    List<ScannedTransaction> scanNewBlocks(long lastScannedBlock);

    /**
     * Get current block height.
     */
    long getCurrentBlockHeight();

    /**
     * Get transaction confirmations.
     */
    int getConfirmations(String txHash);

    /**
     * Check if node is healthy.
     */
    boolean isHealthy();

    /**
     * Which chain this adapter handles.
     */
    com.nexusflow.domain.shared.Chain supportedChain();
}