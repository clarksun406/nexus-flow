package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataMnemonicBackupRepository extends JpaRepository<MnemonicBackupEntity, String> {

    Optional<MnemonicBackupEntity> findByWalletId(String walletId);
}
