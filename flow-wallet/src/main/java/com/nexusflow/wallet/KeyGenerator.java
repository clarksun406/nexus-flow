package com.nexusflow.wallet;

import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Key generation and chain-specific address derivation.
 *
 * Phase 1: per-wallet random private keys with real ETH/TRON address derivation.
 * Phase 2+: replace random generation with BIP32/BIP44 HD derivation (see roadmap P1-3).
 */
@Slf4j
@Component
public class KeyGenerator {

    /** TRON mainnet address prefix byte (0x41). */
    private static final byte TRON_ADDRESS_PREFIX = 0x41;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new private key for the given chain.
     * Returns hex-encoded 256-bit key (no 0x prefix).
     */
    public String generatePrivateKey(Chain chain) {
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return bytesToHex(key);
    }

    /**
     * Derive the on-chain address from a private key.
     *
     * <ul>
     *   <li><b>ETH</b>: keccak256(pubkey)[12:] → EIP-55 checksummed hex</li>
     *   <li><b>TRON</b>: 0x41 ‖ keccak256(pubkey)[12:] → Base58Check</li>
     * </ul>
     *
     * @throws UnsupportedOperationException for chains without derivation support yet (BTC/SOLANA)
     */
    public String deriveAddress(String privateKey, Chain chain) {
        ECKeyPair keyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        byte[] hash20 = Numeric.hexStringToByteArray(Keys.getAddress(keyPair.getPublicKey()));

        return switch (chain) {
            case ETH -> Keys.toChecksumAddress(Numeric.toHexString(hash20));
            case TRON -> toTronAddress(hash20);
            default -> throw new UnsupportedOperationException(
                    "Address derivation not supported for chain: " + chain);
        };
    }

    private String toTronAddress(byte[] hash20) {
        byte[] payload = new byte[1 + hash20.length];
        payload[0] = TRON_ADDRESS_PREFIX;
        System.arraycopy(hash20, 0, payload, 1, hash20.length);
        return Base58.encodeChecked(payload);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
