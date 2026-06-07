package com.nexusflow.wallet;

import com.nexusflow.application.WalletApplicationService;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * High-level wallet service for creating and managing wallets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final KeyGenerator keyGenerator;
    private final WalletApplicationService walletAppService;

    /**
     * Create a new hot wallet for the given chain.
     * Generates keys, encrypts the private key, and persists.
     */
    public Wallet createHotWallet(String name, Chain chain) {
        String privateKey = keyGenerator.generatePrivateKey(chain);
        String address = keyGenerator.deriveAddress(privateKey, chain);

        Wallet wallet = walletAppService.registerWallet(
                name, chain, WalletType.HOT, address, privateKey);

        log.info("Hot wallet created: name={}, chain={}, address={}", name, chain, address);
        return wallet;
    }
}