package com.nexusflow.application;

import com.nexusflow.common.OrphanTransactionNotFoundException;
import com.nexusflow.domain.blockchain.OrphanTransaction;
import com.nexusflow.domain.blockchain.OrphanTransactionRepository;
import com.nexusflow.domain.blockchain.OrphanTransactionStatus;
import com.nexusflow.domain.shared.Chain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrphanTransactionApplicationServiceTest {

    private OrphanTransactionRepository repository;
    private OrphanTransactionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrphanTransactionRepository.class);
        service = new OrphanTransactionApplicationService(repository);
    }

    @Test
    void listReturnsResponsesForStatus() {
        when(repository.findByStatus(OrphanTransactionStatus.UNMATCHED))
                .thenReturn(List.of(orphan()));

        var responses = service.list(OrphanTransactionStatus.UNMATCHED);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTxHash()).isEqualTo("tx-1");
        assertThat(responses.get(0).getStatus()).isEqualTo("UNMATCHED");
    }

    @Test
    void resolveMarksTransactionResolved() {
        OrphanTransaction orphan = orphan();
        when(repository.findByChainAndTxHash(Chain.TRON, "tx-1")).thenReturn(Optional.of(orphan));

        var response = service.resolve(Chain.TRON, "tx-1", "pay-1");

        assertThat(response.getStatus()).isEqualTo("RESOLVED");
        assertThat(response.getResolvedPaymentId()).isEqualTo("pay-1");
        verify(repository).save(orphan);
    }

    @Test
    void ignoreMarksTransactionIgnored() {
        OrphanTransaction orphan = orphan();
        when(repository.findByChainAndTxHash(Chain.TRON, "tx-1")).thenReturn(Optional.of(orphan));

        var response = service.ignore(Chain.TRON, "tx-1");

        assertThat(response.getStatus()).isEqualTo("IGNORED");
        verify(repository).save(orphan);
    }

    @Test
    void resolveThrowsWhenTransactionIsMissing() {
        when(repository.findByChainAndTxHash(Chain.TRON, "tx-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(Chain.TRON, "tx-1", "pay-1"))
                .isInstanceOf(OrphanTransactionNotFoundException.class);
    }

    private OrphanTransaction orphan() {
        return OrphanTransaction.builder()
                .id("orphan-1")
                .chain(Chain.TRON)
                .txHash("tx-1")
                .toAddress("TADDR")
                .amount("100")
                .currency("USDT_TRC20")
                .blockNumber(123L)
                .build();
    }
}
