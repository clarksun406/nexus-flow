package com.nexusflow.domain.wallet;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
public class MnemonicBackup {

    private String id;
    private String walletId;
    private Chain chain;
    private String encryptedMnemonic;
    private String derivationPath;
    private Instant createdAt;

    @Builder
    public MnemonicBackup(String id, String walletId, Chain chain,
                          String encryptedMnemonic, String derivationPath) {
        this.id = id;
        this.walletId = walletId;
        this.chain = chain;
        this.encryptedMnemonic = encryptedMnemonic;
        this.derivationPath = derivationPath;
        this.createdAt = Instant.now();
    }

    @Builder(builderMethodName = "reconstitute", builderClassName = "MnemonicBackupReconstituteBuilder")
    private MnemonicBackup(String id, String walletId, Chain chain,
                           String encryptedMnemonic, String derivationPath,
                           Instant createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.chain = chain;
        this.encryptedMnemonic = encryptedMnemonic;
        this.derivationPath = derivationPath;
        this.createdAt = createdAt;
    }
}
