package com.nexusflow.domain.wallet;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
public class AddressPoolEntry {

    private String id;
    private Chain chain;
    private String address;
    private String encryptedPrivateKey;
    private String derivationPath;
    private Integer derivationIndex;
    private AddressPoolStatus status;
    private String assignedPaymentId;
    private Instant createdAt;
    private Instant assignedAt;
    private Instant updatedAt;

    @Builder
    public AddressPoolEntry(String id, Chain chain, String address, String encryptedPrivateKey,
                            String derivationPath, Integer derivationIndex) {
        this.id = id;
        this.chain = chain;
        this.address = address;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.derivationPath = derivationPath;
        this.derivationIndex = derivationIndex;
        this.status = AddressPoolStatus.AVAILABLE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @Builder(builderMethodName = "reconstitute", builderClassName = "AddressPoolEntryReconstituteBuilder")
    private AddressPoolEntry(String id, Chain chain, String address, String encryptedPrivateKey,
                             String derivationPath, Integer derivationIndex, AddressPoolStatus status,
                             String assignedPaymentId, Instant createdAt, Instant assignedAt,
                             Instant updatedAt) {
        this.id = id;
        this.chain = chain;
        this.address = address;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.derivationPath = derivationPath;
        this.derivationIndex = derivationIndex;
        this.status = status;
        this.assignedPaymentId = assignedPaymentId;
        this.createdAt = createdAt;
        this.assignedAt = assignedAt;
        this.updatedAt = updatedAt;
    }

    public void assignTo(String paymentId) {
        if (status != AddressPoolStatus.AVAILABLE) {
            throw new IllegalStateException("Address is not available: " + address);
        }
        this.status = AddressPoolStatus.ASSIGNED;
        this.assignedPaymentId = paymentId;
        this.assignedAt = Instant.now();
        touch();
    }

    public void disable() {
        this.status = AddressPoolStatus.DISABLED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
