package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "merchant_credentials")
public class MerchantCredentialEntity {

    @Id
    @Column(name = "credential_id", length = 64)
    private String credentialId;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "key_hash", length = 128, nullable = false, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", length = 32, nullable = false)
    private String keyPrefix;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "create_time")
    private Instant createTime;

    @Column(name = "update_time")
    private Instant updateTime;
}
