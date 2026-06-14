package com.nexusflow.wallet;

import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * BIP39/BIP44 key generation and chain-specific address derivation.
 */
@Slf4j
@Component
public class KeyGenerator {

    private static final byte TRON_ADDRESS_PREFIX = 0x41;
    private static final int HARDENED = Bip32ECKeyPair.HARDENED_BIT;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a private key from a fresh BIP39 mnemonic.
     * Use generateMnemonic + derivePrivateKey when the seed phrase must be backed up.
     */
    public String generatePrivateKey(Chain chain) {
        return derivePrivateKey(generateMnemonic(), chain, 0);
    }

    /**
     * Generate a 12-word BIP39 mnemonic.
     */
    public String generateMnemonic() {
        byte[] entropy = new byte[16];
        secureRandom.nextBytes(entropy);
        return MnemonicUtils.generateMnemonic(entropy);
    }

    /**
     * Derive account 0 external-chain key at the given index.
     */
    public String derivePrivateKey(String mnemonic, Chain chain, int index) {
        return derivePrivateKey(mnemonic, "", chain, 0, index);
    }

    /**
     * Derive a private key using the chain's BIP44 path:
     * ETH m/44'/60'/account'/0/index, TRON m/44'/195'/account'/0/index,
     * BTC m/44'/0'/account'/0/index.
     */
    public String derivePrivateKey(String mnemonic, String passphrase, Chain chain, int account, int index) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, passphrase != null ? passphrase : "");
        Bip32ECKeyPair master = Bip32ECKeyPair.generateKeyPair(seed);
        Bip32ECKeyPair derived = Bip32ECKeyPair.deriveKeyPair(master, derivationPath(chain, account, index));
        return Numeric.toHexStringNoPrefixZeroPadded(derived.getPrivateKey(), 64);
    }

    public String derivationPathText(Chain chain, int account, int index) {
        return "m/44'/" + coinType(chain) + "'/" + account + "'/0/" + index;
    }

    /**
     * Derive the on-chain address from a private key.
     */
    public String deriveAddress(String privateKey, Chain chain) {
        ECKeyPair keyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        byte[] hash20 = Numeric.hexStringToByteArray(Keys.getAddress(keyPair.getPublicKey()));

        return switch (chain) {
            case ETH -> Keys.toChecksumAddress(Numeric.toHexString(hash20));
            case TRON -> toTronAddress(hash20);
            case BTC -> toBitcoinP2pkhAddress(keyPair);
            case SOLANA -> throw new UnsupportedOperationException(
                    "Address derivation not supported for chain: " + chain);
        };
    }

    public String deriveAddressFromMnemonic(String mnemonic, Chain chain, int index) {
        return deriveAddress(derivePrivateKey(mnemonic, chain, index), chain);
    }

    private String toTronAddress(byte[] hash20) {
        byte[] payload = new byte[1 + hash20.length];
        payload[0] = TRON_ADDRESS_PREFIX;
        System.arraycopy(hash20, 0, payload, 1, hash20.length);
        return Base58.encodeChecked(payload);
    }

    private String toBitcoinP2pkhAddress(ECKeyPair keyPair) {
        byte[] payload = new byte[21];
        payload[0] = 0x00; // mainnet P2PKH
        byte[] hash160 = hash160(compressedPublicKey(keyPair.getPublicKey()));
        System.arraycopy(hash160, 0, payload, 1, hash160.length);
        return Base58.encodeChecked(payload);
    }

    private int[] derivationPath(Chain chain, int account, int index) {
        return new int[] {
                44 | HARDENED,
                coinType(chain) | HARDENED,
                account | HARDENED,
                0,
                index
        };
    }

    private int coinType(Chain chain) {
        return switch (chain) {
            case ETH -> 60;
            case TRON -> 195;
            case BTC -> 0;
            case SOLANA -> throw new UnsupportedOperationException(
                    "HD derivation not supported for chain: " + chain);
        };
    }

    private byte[] compressedPublicKey(BigInteger publicKey) {
        byte[] raw = Numeric.toBytesPadded(publicKey, 64);
        byte[] x = Arrays.copyOfRange(raw, 0, 32);
        byte[] y = Arrays.copyOfRange(raw, 32, 64);
        byte[] compressed = new byte[33];
        compressed[0] = (byte) ((y[31] & 1) == 0 ? 0x02 : 0x03);
        System.arraycopy(x, 0, compressed, 1, 32);
        return compressed;
    }

    private byte[] hash160(byte[] input) {
        byte[] sha = sha256(input);
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(sha, 0, sha.length);
        byte[] out = new byte[20];
        digest.doFinal(out, 0);
        return out;
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
