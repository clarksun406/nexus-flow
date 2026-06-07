package com.nexusflow.infra.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tron blockchain adapter (TRC20), backed by the TronGrid HTTP API via {@link TronGridClient}.
 *
 * Block-height, confirmation, and health queries are implemented and unit-tested (response parsing).
 * Block scanning is intentionally NOT implemented (see {@link #scanNewBlocks}).
 */
@Slf4j
public class TronAdapter implements BlockchainAdapter {

    private final TronGridClient client;
    private final String usdtContractAddress;

    public TronAdapter(TronGridClient client, String usdtContractAddress) {
        this.client = client;
        this.usdtContractAddress = usdtContractAddress;
    }

    /**
     * NOT IMPLEMENTED. TronGrid's TRC20 transfer API is account/timestamp-scoped, which does not map
     * onto this block-range abstraction; correct TRC20 block scanning needs the event-info endpoint
     * or full-node block iteration with contract-log decoding. Left as an explicit stub rather than
     * shipping plausible-but-unverified scanning logic (roadmap P0-1).
     */
    @Override
    public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
        log.debug("TRON scanNewBlocks not implemented (from block {}, contract {})",
                lastScannedBlock, usdtContractAddress);
        return Collections.emptyList();
    }

    @Override
    public long getCurrentBlockHeight() {
        try {
            JsonNode node = client.post("/wallet/getnowblock", Map.of());
            return node.path("block_header").path("raw_data").path("number").asLong(0);
        } catch (Exception e) {
            log.error("Failed to get TRON block height: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int getConfirmations(String txHash) {
        try {
            JsonNode info = client.post("/wallet/gettransactioninfobyid", Map.of("value", txHash));
            long txBlock = info.path("blockNumber").asLong(0);
            if (txBlock <= 0) {
                return 0; // tx not yet packed into a block (or unknown)
            }
            long current = getCurrentBlockHeight();
            return current > txBlock ? (int) (current - txBlock) : 0;
        } catch (Exception e) {
            log.error("Failed to get TRON confirmations for {}: {}", txHash, e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean isHealthy() {
        return getCurrentBlockHeight() > 0;
    }

    @Override
    public Chain supportedChain() {
        return Chain.TRON;
    }
}
