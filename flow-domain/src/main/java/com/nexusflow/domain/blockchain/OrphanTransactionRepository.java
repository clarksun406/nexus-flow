package com.nexusflow.domain.blockchain;

import com.nexusflow.domain.shared.Chain;

import java.util.List;
import java.util.Optional;

public interface OrphanTransactionRepository {

    void save(OrphanTransaction transaction);

    Optional<OrphanTransaction> findByChainAndTxHash(Chain chain, String txHash);

    List<OrphanTransaction> findByStatus(OrphanTransactionStatus status);
}
