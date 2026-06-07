package com.nexusflow.wallet;

import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Key generation utility for blockchain wallets.
 *
 * Phase 1 MVP: Uses simple random key generation.
 * Phase 2+: Replace with BIP32/BIP44 HD wallet derivation.
 */
@Slf4j
@Component
public class KeyGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new private key for the given chain.
     * Returns hex-encoded 256-bit key.
     */
    public String generatePrivateKey(Chain chain) {
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return bytesToHex(key);
    }

    /**
     * Derive address from private key for the given chain.
     * Phase 1: Placeholder - in production, use chain-specific derivation.
     */
    public String deriveAddress(String privateKey, Chain chain) {
        // TODO: Implement chain-specific address derivation
        // Tron: ECKey -> base58check
        // ETH: ECKey -> hex
        // BTC: ECKey -> base58check
        log.warn("Address derivation not yet implemented for chain: {}", chain);
        return "0x" + privateKey.substring(0, 40);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}