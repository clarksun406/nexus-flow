package com.nexusflow.wallet;

import com.nexusflow.application.WalletApplicationService;
import com.nexusflow.common.AesGcmEncryption;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.MnemonicBackup;
import com.nexusflow.domain.wallet.MnemonicStore;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * High-level wallet service for creating and managing wallets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final KeyGenerator keyGenerator;
    private final WalletApplicationService walletAppService;
    private final MnemonicStore mnemonicStore;
    private final AesGcmEncryption encryption;

    /**
     * Create a new hot wallet for the given chain.
     * Generates keys, encrypts the private key, and persists.
     */
    public Wallet createHotWallet(String name, Chain chain) {
        String mnemonic = keyGenerator.generateMnemonic();
        String derivationPath = keyGenerator.derivationPathText(chain, 0, 0);
        String privateKey = keyGenerator.derivePrivateKey(mnemonic, chain, 0);
        String address = keyGenerator.deriveAddress(privateKey, chain);

        Wallet wallet = walletAppService.registerWallet(
                name, chain, WalletType.HOT, address, privateKey);

        mnemonicStore.save(MnemonicBackup.builder()
                .id(UUID.randomUUID().toString())
                .walletId(wallet.getId())
                .chain(chain)
                .encryptedMnemonic(encryption.encrypt(mnemonic))
                .derivationPath(derivationPath)
                .build());

        log.info("Hot wallet created: name={}, chain={}, address={}", name, chain, address);
        return wallet;
    }
}
