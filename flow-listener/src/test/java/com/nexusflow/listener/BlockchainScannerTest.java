package com.nexusflow.listener;

import com.nexusflow.application.BlockchainCircuitBreaker;
import com.nexusflow.application.PaymentApplicationService;
import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ChainScanCursor;
import com.nexusflow.domain.blockchain.ChainScanCursorRepository;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.infra.blockchain.BlockchainAdapterRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockchainScannerTest {

    @Test
    void initializesCursorProcessesTransactionsAndAdvancesToTip() {
        FakeBlockchainAdapter adapter = new FakeBlockchainAdapter(Chain.ETH, 102L);
        adapter.hashes.put(101L, "hash-101");
        adapter.hashes.put(102L, "hash-102");
        ScannedTransaction tx = ScannedTransaction.builder()
                .txHash("0xtx")
                .toAddress("0xaddress")
                .amount("100")
                .blockNumber(102L)
                .build();
        adapter.transactions.add(tx);
        InMemoryCursorRepository cursorRepository = new InMemoryCursorRepository();
        RecordingProcessor processor = new RecordingProcessor();

        scanner(adapter, cursorRepository, processor, new RecordingPaymentService(), 12L).scanAll();

        assertEquals(List.of(101L), adapter.scanStarts);
        assertEquals(List.of(tx), processor.processedTransactions);
        ChainScanCursor saved = cursorRepository.findByChain(Chain.ETH).orElseThrow();
        assertEquals(102L, saved.getLastScannedBlock());
        assertEquals("hash-102", saved.getLastScannedBlockHash());
    }

    @Test
    void rewindsCursorAndRollsBackPaymentsWhenStoredHashDiffersFromCanonicalHash() {
        FakeBlockchainAdapter adapter = new FakeBlockchainAdapter(Chain.ETH, 105L);
        adapter.hashes.put(88L, "hash-88");
        adapter.hashes.put(100L, "canonical-100");
        adapter.hashes.put(105L, "hash-105");
        InMemoryCursorRepository cursorRepository = new InMemoryCursorRepository();
        cursorRepository.save(ChainScanCursor.builder()
                .chain(Chain.ETH)
                .lastScannedBlock(100L)
                .lastScannedBlockHash("stale-100")
                .build());
        RecordingPaymentService paymentService = new RecordingPaymentService();

        scanner(adapter, cursorRepository, new RecordingProcessor(), paymentService, 12L).scanAll();

        assertEquals(List.of(88L), adapter.scanStarts);
        assertEquals(Chain.ETH, paymentService.rollbackChain);
        assertEquals(88L, paymentService.rollbackForkBlock);
        ChainScanCursor saved = cursorRepository.findByChain(Chain.ETH).orElseThrow();
        assertEquals(105L, saved.getLastScannedBlock());
        assertEquals("hash-105", saved.getLastScannedBlockHash());
    }

    private BlockchainScanner scanner(FakeBlockchainAdapter adapter,
                                      ChainScanCursorRepository cursorRepository,
                                      TransactionProcessor processor,
                                      PaymentApplicationService paymentService,
                                      long rewindBlocks) {
        return new BlockchainScanner(
                new BlockchainAdapterRegistry(List.of(adapter)),
                processor,
                cursorRepository,
                paymentService,
                new BlockchainCircuitBreaker(3, 30),
                rewindBlocks);
    }

    private static final class FakeBlockchainAdapter implements BlockchainAdapter {
        private final Chain chain;
        private final long currentHeight;
        private final Map<Long, String> hashes = new HashMap<>();
        private final List<ScannedTransaction> transactions = new ArrayList<>();
        private final List<Long> scanStarts = new ArrayList<>();

        private FakeBlockchainAdapter(Chain chain, long currentHeight) {
            this.chain = chain;
            this.currentHeight = currentHeight;
        }

        @Override
        public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
            scanStarts.add(lastScannedBlock);
            return List.copyOf(transactions);
        }

        @Override
        public long getCurrentBlockHeight() {
            return currentHeight;
        }

        @Override
        public int getConfirmations(String txHash) {
            return 0;
        }

        @Override
        public String getBlockHash(long blockNumber) {
            return hashes.get(blockNumber);
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public Chain supportedChain() {
            return chain;
        }
    }

    private static final class InMemoryCursorRepository implements ChainScanCursorRepository {
        private final Map<Chain, ChainScanCursor> cursors = new HashMap<>();

        @Override
        public void save(ChainScanCursor cursor) {
            cursors.put(cursor.getChain(), cursor);
        }

        @Override
        public Optional<ChainScanCursor> findByChain(Chain chain) {
            return Optional.ofNullable(cursors.get(chain));
        }
    }

    private static final class RecordingProcessor extends TransactionProcessor {
        private final List<ScannedTransaction> processedTransactions = new ArrayList<>();
        private final List<Chain> processedChains = new ArrayList<>();

        private RecordingProcessor() {
            super(null, null, null);
        }

        @Override
        public void process(ScannedTransaction tx, Chain chain) {
            processedTransactions.add(tx);
            processedChains.add(chain);
        }
    }

    private static final class RecordingPaymentService extends PaymentApplicationService {
        private Chain rollbackChain;
        private long rollbackForkBlock = -1L;

        private RecordingPaymentService() {
            super(null, null, null);
        }

        @Override
        public void rollbackPaymentsAfterReorg(Chain chain, long forkBlock) {
            rollbackChain = chain;
            rollbackForkBlock = forkBlock;
        }
    }
}
