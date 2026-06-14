package com.nexusflow.infra.persistence;

import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory wallet repository for Phase 1 MVP.
 * Replace with JPA/MyBatis-Plus implementation for production.
 */
@Repository
@ConditionalOnProperty(name = "nexusflow.execution.persistence", havingValue = "memory")
public class InMemoryWalletRepository implements WalletRepository {

    private final Map<String, Wallet> byId = new ConcurrentHashMap<>();

    @Override
    public void save(Wallet wallet) {
        byId.put(wallet.getId(), wallet);
    }

    @Override
    public Optional<Wallet> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Wallet> findActiveByChain(Chain chain) {
        return byId.values().stream()
                .filter(w -> w.getChain() == chain && w.isActive())
                .findFirst();
    }

    @Override
    public List<Wallet> findAllActive() {
        return byId.values().stream()
                .filter(Wallet::isActive)
                .toList();
    }

    @Override
    public Optional<Wallet> findByAddress(String address) {
        return byId.values().stream()
                .filter(w -> w.getAddress().equals(address))
                .findFirst();
    }
}
