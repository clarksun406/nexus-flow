package com.nexusflow.domain.wallet;

import com.nexusflow.domain.shared.Chain;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Wallet aggregate.
 */
public interface WalletRepository {

    void save(Wallet wallet);

    Optional<Wallet> findById(String id);

    Optional<Wallet> findActiveByChain(Chain chain);

    List<Wallet> findAllActive();

    Optional<Wallet> findByAddress(String address);
}