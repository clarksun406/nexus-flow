package com.nexusflow.application;

import com.nexusflow.common.AesGcmEncryption;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletRepository;
import com.nexusflow.domain.wallet.WalletType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for wallet management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletApplicationService {

    private final WalletRepository walletRepository;
    private final AesGcmEncryption encryption;

    /**
     * Register a new wallet with encrypted private key.
     */
    @Transactional
    public Wallet registerWallet(String name, Chain chain, WalletType type,
                                  String address, String plainPrivateKey) {
        String encrypted = encryption.encrypt(plainPrivateKey);

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .chain(chain)
                .type(type)
                .address(address)
                .encryptedPrivateKey(encrypted)
                .build();

        walletRepository.save(wallet);
        log.info("Wallet registered: name={}, chain={}, address={}", name, chain, address);
        return wallet;
    }

    /**
     * Decrypt and return private key (use with extreme caution).
     */
    public String decryptPrivateKey(String walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NexusFlowException(ErrorCode.WALLET_NOT_FOUND,
                        "Wallet not found: " + walletId));
        return encryption.decrypt(wallet.getEncryptedPrivateKey());
    }

    /**
     * List all active wallets.
     */
    @Transactional(readOnly = true)
    public List<Wallet> listActiveWallets() {
        return walletRepository.findAllActive();
    }

    /**
     * Deactivate a wallet.
     */
    @Transactional
    public void deactivateWallet(String walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NexusFlowException(ErrorCode.WALLET_NOT_FOUND,
                        "Wallet not found: " + walletId));
        wallet.deactivate();
        walletRepository.save(wallet);
        log.info("Wallet deactivated: {}", walletId);
    }
}