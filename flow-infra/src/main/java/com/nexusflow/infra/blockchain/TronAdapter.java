package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * Tron blockchain adapter (TRC20).
 *
 * Uses TronGrid API or full node for block scanning.
 * Phase 1 MVP: HTTP-based polling of TronGrid API.
 */
@Slf4j
public class TronAdapter implements BlockchainAdapter {

    private final String nodeUrl;
    private final String usdtContractAddress;

    public TronAdapter(String nodeUrl, String usdtContractAddress) {
        this.nodeUrl = nodeUrl;
        this.usdtContractAddress = usdtContractAddress;
    }

    @Override
    public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
        // TODO Phase 1: Implement via TronGrid API
        // 1. GET /v1/blocks?limit=N
        // 2. Filter transactions involving our addresses
        // 3. Parse TRC20 transfers from contract call data
        log.debug("Scanning TRON blocks from {} via {}", lastScannedBlock, nodeUrl);
        return Collections.emptyList();
    }

    @Override
    public long getCurrentBlockHeight() {
        // TODO: GET /wallet/getnowblock
        return 0;
    }

    @Override
    public int getConfirmations(String txHash) {
        // TODO: currentBlock - txBlock
        return 0;
    }

    @Override
    public boolean isHealthy() {
        // TODO: health check to node
        return true;
    }

    @Override
    public Chain supportedChain() {
        return Chain.TRON;
    }
}