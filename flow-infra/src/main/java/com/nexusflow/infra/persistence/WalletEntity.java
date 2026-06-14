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
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 20, nullable = false)
    private String chain;

    @Column(length = 10, nullable = false)
    private String type;

    @Column(length = 256, nullable = false, unique = true)
    private String address;

    @Column(name = "encrypted_private_key", columnDefinition = "TEXT", nullable = false)
    private String encryptedPrivateKey;

    @Column(name = "kms_key_id", length = 256)
    private String kmsKeyId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}
