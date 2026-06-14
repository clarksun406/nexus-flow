package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataWalletRepository extends JpaRepository<WalletEntity, String> {

    Optional<WalletEntity> findFirstByChainAndActive(String chain, boolean active);

    List<WalletEntity> findByActive(boolean active);

    Optional<WalletEntity> findByAddress(String address);
}
