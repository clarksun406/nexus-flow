package com.nexusflow.domain.wallet;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Wallet aggregate root.
 *
 * Represents a hot or cold wallet for a specific blockchain.
 * Private keys are stored encrypted; never exposed in plaintext.
 */
@Getter
public class Wallet {

    private String id;
    private String name;
    private Chain chain;
    private WalletType type;
    private String address;
    private String encryptedPrivateKey; // AES-256-GCM encrypted
    private String kmsKeyId;            // external KMS reference (optional)
    private String mpcWalletId;         // external MPC provider wallet/vault id (optional)
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder
    public Wallet(String id, String name, Chain chain, WalletType type,
                  String address, String encryptedPrivateKey, String kmsKeyId,
                  String mpcWalletId) {
        this.id = id;
        this.name = name;
        this.chain = chain;
        this.type = type;
        this.address = address;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.kmsKeyId = kmsKeyId;
        this.mpcWalletId = mpcWalletId;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Full-args builder for reconstituting a Wallet from persistence.
     * Not for public use - only for repository mapping.
     */
    @Builder(builderMethodName = "reconstitute", builderClassName = "WalletReconstituteBuilder")
    private Wallet(String id, String name, Chain chain, WalletType type,
                   String address, String encryptedPrivateKey, String kmsKeyId,
                   String mpcWalletId, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.chain = chain;
        this.type = type;
        this.address = address;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.kmsKeyId = kmsKeyId;
        this.mpcWalletId = mpcWalletId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }
}
