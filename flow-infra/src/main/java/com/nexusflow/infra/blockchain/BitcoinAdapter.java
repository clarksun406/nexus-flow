package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * Bitcoin blockchain adapter (UTXO model).
 * Phase 2 target.
 */
@Slf4j
public class BitcoinAdapter implements BlockchainAdapter {

    private final String nodeUrl;

    public BitcoinAdapter(String nodeUrl) {
        this.nodeUrl = nodeUrl;
    }

    @Override
    public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
        log.debug("BTC scanning not yet implemented");
        return Collections.emptyList();
    }

    @Override
    public long getCurrentBlockHeight() {
        return 0;
    }

    @Override
    public int getConfirmations(String txHash) {
        return 0;
    }

    @Override
    public boolean isHealthy() {
        return false;
    }

    @Override
    public Chain supportedChain() {
        return Chain.BTC;
    }
}