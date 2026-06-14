package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataOrphanTransactionRepository extends JpaRepository<OrphanTransactionEntity, String> {

    Optional<OrphanTransactionEntity> findByChainAndTxHash(String chain, String txHash);

    List<OrphanTransactionEntity> findByStatus(String status);
}
