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
     * Get the canonical block hash at a given height.
     * Adapters that cannot query historical block hashes may return {@code null}.
     */
    default String getBlockHash(long blockNumber) {
        return null;
    }

    /**
     * Check if node is healthy.
     */
    boolean isHealthy();

    /**
     * Which chain this adapter handles.
     */
    com.nexusflow.domain.shared.Chain supportedChain();
}
