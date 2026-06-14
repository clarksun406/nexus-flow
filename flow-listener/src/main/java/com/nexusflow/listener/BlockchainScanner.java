package com.nexusflow.listener;

import com.nexusflow.application.BlockchainCircuitBreaker;
import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ChainScanCursor;
import com.nexusflow.domain.blockchain.ChainScanCursorRepository;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.infra.blockchain.BlockchainAdapterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled blockchain scanner.
 *
 * Polls each chain adapter periodically for new blocks,
 * detects incoming transactions, and triggers payment processing.
 */
@Slf4j
@Component
public class BlockchainScanner {

    private final BlockchainAdapterRegistry adapterRegistry;
    private final TransactionProcessor processor;
    private final ChainScanCursorRepository cursorRepository;
    private final PaymentApplicationService paymentService;
    private final BlockchainCircuitBreaker circuitBreaker;
    private final long reorgRewindBlocks;

    public BlockchainScanner(BlockchainAdapterRegistry adapterRegistry,
                             TransactionProcessor processor,
                             ChainScanCursorRepository cursorRepository,
                             PaymentApplicationService paymentService,
                             BlockchainCircuitBreaker circuitBreaker,
                             @Value("${nexusflow.scanner.reorg-rewind-blocks:12}") long reorgRewindBlocks) {
        this.adapterRegistry = adapterRegistry;
        this.processor = processor;
        this.cursorRepository = cursorRepository;
        this.paymentService = paymentService;
        this.circuitBreaker = circuitBreaker;
        this.reorgRewindBlocks = reorgRewindBlocks;
    }

    /**
     * Scan all chains every 15 seconds.
     */
    @Scheduled(fixedDelayString = "${nexusflow.scanner.interval-ms:15000}")
    public void scanAll() {
        for (BlockchainAdapter adapter : adapterRegistry.getAllAdapters()) {
            try {
                scanChain(adapter);
            } catch (Exception e) {
                circuitBreaker.recordFailure(adapter.supportedChain());
                log.error("Scan failed for chain {}: {}", adapter.supportedChain(), e.getMessage(), e);
            }
        }
    }

    private void scanChain(BlockchainAdapter adapter) {
        if (!adapter.isHealthy()) {
            log.debug("Skipping scan for unhealthy chain: {}", adapter.supportedChain());
            return;
        }

        Chain chain = adapter.supportedChain();
        if (!circuitBreaker.allowRequest(chain)) {
            log.debug("Skipping scan for open circuit breaker: {}", chain);
            return;
        }
        long current = adapter.getCurrentBlockHeight();
        if (current <= 0) {
            circuitBreaker.recordFailure(chain);
            return;
        }
        ChainScanCursor cursor = cursorRepository.findByChain(chain)
                .orElseGet(() -> ChainScanCursor.builder()
                        .chain(chain)
                        .lastScannedBlock(Math.max(0, current - 1))
                        .lastScannedBlockHash(adapter.getBlockHash(Math.max(0, current - 1)))
                        .build());

        cursor = handleReorgIfNeeded(adapter, cursor);
        long lastBlock = cursor.getLastScannedBlock();

        var transactions = adapter.scanNewBlocks(lastBlock);

        if (!transactions.isEmpty()) {
            log.info("Scanned {} transactions on {} from block {}",
                    transactions.size(), chain, lastBlock);
            for (ScannedTransaction tx : transactions) {
                processor.process(tx, chain);
            }
        }

        long newCurrent = adapter.getCurrentBlockHeight();
        cursor.advanceTo(newCurrent, adapter.getBlockHash(newCurrent));
        cursorRepository.save(cursor);
        circuitBreaker.recordSuccess(chain);
    }

    private ChainScanCursor handleReorgIfNeeded(BlockchainAdapter adapter, ChainScanCursor cursor) {
        if (cursor.getLastScannedBlock() <= 0 || cursor.getLastScannedBlockHash() == null) {
            return cursor;
        }
        String canonicalHash = adapter.getBlockHash(cursor.getLastScannedBlock());
        if (canonicalHash == null || canonicalHash.equalsIgnoreCase(cursor.getLastScannedBlockHash())) {
            return cursor;
        }

        String oldHash = cursor.getLastScannedBlockHash();
        long forkBlock = Math.max(0, cursor.getLastScannedBlock() - reorgRewindBlocks);
        String forkHash = adapter.getBlockHash(forkBlock);
        cursor.rewindTo(forkBlock, forkHash);
        cursorRepository.save(cursor);
        paymentService.rollbackPaymentsAfterReorg(adapter.supportedChain(), forkBlock);
        log.warn("Chain reorg detected: chain={}, rewoundTo={}, oldHash={}, canonicalHash={}",
                adapter.supportedChain(), forkBlock, oldHash, canonicalHash);
        return cursor;
    }
}
