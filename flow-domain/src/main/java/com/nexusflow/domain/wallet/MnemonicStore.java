package com.nexusflow.domain.wallet;

import java.util.Optional;

public interface MnemonicStore {

    void save(MnemonicBackup backup);

    Optional<MnemonicBackup> findByWalletId(String walletId);
}
