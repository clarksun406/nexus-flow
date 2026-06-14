package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "mnemonic_backups")
public class MnemonicBackupEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "wallet_id", length = 64, nullable = false, unique = true)
    private String walletId;

    @Column(length = 20, nullable = false)
    private String chain;

    @Column(name = "encrypted_mnemonic", columnDefinition = "TEXT", nullable = false)
    private String encryptedMnemonic;

    @Column(name = "derivation_path", length = 64, nullable = false)
    private String derivationPath;

    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    private Long version;
}
