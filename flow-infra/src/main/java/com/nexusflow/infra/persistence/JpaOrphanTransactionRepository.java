package com.nexusflow.infra.persistence;

import com.nexusflow.domain.blockchain.OrphanTransaction;
import com.nexusflow.domain.blockchain.OrphanTransactionRepository;
import com.nexusflow.domain.blockchain.OrphanTransactionStatus;
import com.nexusflow.domain.shared.Chain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaOrphanTransactionRepository implements OrphanTransactionRepository {

    private final SpringDataOrphanTransactionRepository repository;

    @Override
    public void save(OrphanTransaction transaction) {
        OrphanTransactionEntity entity = toEntity(transaction);
        repository.findById(transaction.getId())
                .map(OrphanTransactionEntity::getVersion)
                .ifPresent(entity::setVersion);
        repository.save(entity);
    }

    @Override
    public Optional<OrphanTransaction> findByChainAndTxHash(Chain chain, String txHash) {
        return repository.findByChainAndTxHash(chain.name(), txHash).map(this::toDomain);
    }

    @Override
    public List<OrphanTransaction> findByStatus(OrphanTransactionStatus status) {
        return repository.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    OrphanTransactionEntity toEntity(OrphanTransaction transaction) {
        OrphanTransactionEntity entity = new OrphanTransactionEntity();
        entity.setId(transaction.getId());
        entity.setChain(transaction.getChain() != null ? transaction.getChain().name() : null);
        entity.setTxHash(transaction.getTxHash());
        entity.setToAddress(transaction.getToAddress());
        entity.setAmount(transaction.getAmount());
        entity.setCurrency(transaction.getCurrency());
        entity.setBlockNumber(transaction.getBlockNumber());
        entity.setStatus(transaction.getStatus() != null ? transaction.getStatus().name() : null);
        entity.setFirstSeenAt(transaction.getFirstSeenAt());
        entity.setLastSeenAt(transaction.getLastSeenAt());
        entity.setSeenCount(transaction.getSeenCount());
        entity.setResolvedPaymentId(transaction.getResolvedPaymentId());
        return entity;
    }

    OrphanTransaction toDomain(OrphanTransactionEntity entity) {
        return OrphanTransaction.reconstitute()
                .id(entity.getId())
                .chain(entity.getChain() != null ? Chain.valueOf(entity.getChain()) : null)
                .txHash(entity.getTxHash())
                .toAddress(entity.getToAddress())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .blockNumber(entity.getBlockNumber())
                .status(entity.getStatus() != null ? OrphanTransactionStatus.valueOf(entity.getStatus()) : null)
                .firstSeenAt(entity.getFirstSeenAt())
                .lastSeenAt(entity.getLastSeenAt())
                .seenCount(entity.getSeenCount())
                .resolvedPaymentId(entity.getResolvedPaymentId())
                .build();
    }
}
