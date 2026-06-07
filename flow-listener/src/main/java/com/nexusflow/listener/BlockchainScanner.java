package com.nexusflow.listener;

import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.infra.blockchain.BlockchainAdapterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduled blockchain scanner.
 *
 * Polls each chain adapter periodically for new blocks,
 * detects incoming transactions, and triggers payment processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainScanner {

    private final BlockchainAdapterRegistry adapterRegistry;
    private final TransactionProcessor processor;

    // Track last scanned block per chain (in production, persist to DB)
    private final java.util.Map<com.nexusflow.domain.shared.Chain, AtomicLong> lastScannedBlocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Scan all chains every 15 seconds.
     */
    @Scheduled(fixedDelayString = "${nexusflow.scanner.interval-ms:15000}")
    public void scanAll() {
        for (BlockchainAdapter adapter : adapterRegistry.getAllAdapters()) {
            try {
                scanChain(adapter);
            } catch (Exception e) {
                log.error("Scan failed for chain {}: {}", adapter.supportedChain(), e.getMessage(), e);
            }
        }
    }

    private void scanChain(BlockchainAdapter adapter) {
        if (!adapter.isHealthy()) {
            log.debug("Skipping scan for unhealthy chain: {}", adapter.supportedChain());
            return;
        }

        var chain = adapter.supportedChain();
        long lastBlock = lastScannedBlocks.computeIfAbsent(chain,
                        c -> new AtomicLong(adapter.getCurrentBlockHeight() - 1))
                .get();

        var transactions = adapter.scanNewBlocks(lastBlock);

        if (!transactions.isEmpty()) {
            log.info("Scanned {} transactions on {} from block {}",
                    transactions.size(), chain, lastBlock);
            for (ScannedTransaction tx : transactions) {
                processor.process(tx, chain);
            }
        }

        // Update last scanned block
        long current = adapter.getCurrentBlockHeight();
        lastScannedBlocks.get(chain).set(current);
    }
}