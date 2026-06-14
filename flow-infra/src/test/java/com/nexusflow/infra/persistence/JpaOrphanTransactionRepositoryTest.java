package com.nexusflow.infra.persistence;

import com.nexusflow.domain.blockchain.OrphanTransaction;
import com.nexusflow.domain.blockchain.OrphanTransactionStatus;
import com.nexusflow.domain.shared.Chain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaOrphanTransactionRepositoryTest {

    private SpringDataOrphanTransactionRepository springDataRepository;
    private JpaOrphanTransactionRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataOrphanTransactionRepository.class);
        repository = new JpaOrphanTransactionRepository(springDataRepository);
    }

    @Test
    void saveMapsFieldsAndPreservesVersion() {
        OrphanTransactionEntity existing = new OrphanTransactionEntity();
        existing.setId("orphan-1");
        existing.setVersion(7L);
        when(springDataRepository.findById("orphan-1")).thenReturn(Optional.of(existing));
        OrphanTransaction transaction = OrphanTransaction.builder()
                .id("orphan-1")
                .chain(Chain.TRON)
                .txHash("tx-1")
                .toAddress("TADDR")
                .amount("100")
                .currency("USDT_TRC20")
                .blockNumber(123L)
                .build();

        repository.save(transaction);

        ArgumentCaptor<OrphanTransactionEntity> captor = ArgumentCaptor.forClass(OrphanTransactionEntity.class);
        verify(springDataRepository).save(captor.capture());
        OrphanTransactionEntity entity = captor.getValue();
        assertEquals("TRON", entity.getChain());
        assertEquals("tx-1", entity.getTxHash());
        assertEquals("TADDR", entity.getToAddress());
        assertEquals("100", entity.getAmount());
        assertEquals("USDT_TRC20", entity.getCurrency());
        assertEquals(123L, entity.getBlockNumber());
        assertEquals("UNMATCHED", entity.getStatus());
        assertEquals(1, entity.getSeenCount());
        assertEquals(7L, entity.getVersion());
    }

    @Test
    void findByChainAndTxHashReconstitutesDomain() {
        Instant firstSeen = Instant.parse("2026-06-14T00:00:00Z");
        OrphanTransactionEntity entity = entity(firstSeen);
        when(springDataRepository.findByChainAndTxHash("TRON", "tx-1")).thenReturn(Optional.of(entity));

        Optional<OrphanTransaction> found = repository.findByChainAndTxHash(Chain.TRON, "tx-1");

        assertTrue(found.isPresent());
        assertEquals(Chain.TRON, found.get().getChain());
        assertEquals("tx-1", found.get().getTxHash());
        assertEquals(OrphanTransactionStatus.UNMATCHED, found.get().getStatus());
        assertEquals(firstSeen, found.get().getFirstSeenAt());
        assertEquals(2, found.get().getSeenCount());
    }

    @Test
    void findByStatusMapsResults() {
        Instant firstSeen = Instant.parse("2026-06-14T00:00:00Z");
        when(springDataRepository.findByStatus("UNMATCHED")).thenReturn(List.of(entity(firstSeen)));

        List<OrphanTransaction> found = repository.findByStatus(OrphanTransactionStatus.UNMATCHED);

        assertEquals(1, found.size());
        assertEquals("tx-1", found.get(0).getTxHash());
    }

    private OrphanTransactionEntity entity(Instant firstSeen) {
        OrphanTransactionEntity entity = new OrphanTransactionEntity();
        entity.setId("orphan-1");
        entity.setChain("TRON");
        entity.setTxHash("tx-1");
        entity.setToAddress("TADDR");
        entity.setAmount("100");
        entity.setCurrency("USDT_TRC20");
        entity.setBlockNumber(123L);
        entity.setStatus("UNMATCHED");
        entity.setFirstSeenAt(firstSeen);
        entity.setLastSeenAt(firstSeen.plusSeconds(30));
        entity.setSeenCount(2);
        return entity;
    }
}
