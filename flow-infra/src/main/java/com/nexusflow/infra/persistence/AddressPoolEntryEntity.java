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
@Table(name = "address_pool")
public class AddressPoolEntryEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 20, nullable = false)
    private String chain;

    @Column(length = 256, nullable = false, unique = true)
    private String address;

    @Column(name = "encrypted_private_key", columnDefinition = "TEXT", nullable = false)
    private String encryptedPrivateKey;

    @Column(name = "derivation_path", length = 64)
    private String derivationPath;

    @Column(name = "derivation_index")
    private Integer derivationIndex;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "assigned_payment_id", length = 64)
    private String assignedPaymentId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}
