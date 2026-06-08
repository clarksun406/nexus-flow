package com.nexusflow.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmEncryptionTest {

    private final AesGcmEncryption encryption = new AesGcmEncryption(AesGcmEncryption.generateKey());

    @Test
    void encryptDecryptRoundTrip() {
        String plaintext = "my-secret-private-key-12345";
        String encrypted = encryption.encrypt(plaintext);
        String decrypted = encryption.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptedValueDiffersFromPlaintext() {
        String plaintext = "secret";
        String encrypted = encryption.encrypt(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
    }

    @Test
    void samePlaintextProducesDifferentCiphertext() {
        // Random IV means each encryption is unique
        String encrypted1 = encryption.encrypt("same");
        String encrypted2 = encryption.encrypt("same");
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void decryptWithWrongKeyThrows() {
        String encrypted = encryption.encrypt("secret");
        AesGcmEncryption wrongKey = new AesGcmEncryption(AesGcmEncryption.generateKey());
        assertThatThrownBy(() -> wrongKey.decrypt(encrypted))
                .isInstanceOf(NexusFlowException.class);
    }

    @Test
    void decryptWithCorruptedCiphertextThrows() {
        String encrypted = encryption.encrypt("secret");
        // Corrupt the ciphertext by flipping a character
        String corrupted = encrypted.substring(0, encrypted.length() - 2) + "XX";
        assertThatThrownBy(() -> encryption.decrypt(corrupted))
                .isInstanceOf(NexusFlowException.class);
    }

    @Test
    void emptyStringCanBeEncrypted() {
        String encrypted = encryption.encrypt("");
        assertThat(encryption.decrypt(encrypted)).isEmpty();
    }

    @Test
    void unicodeCanBeEncrypted() {
        String plaintext = "私钥-🔐-test";
        assertThat(encryption.decrypt(encryption.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    void generateKeyReturnsBase64() {
        String key = AesGcmEncryption.generateKey();
        assertThat(key).isNotBlank();
        // Base64 of 32 bytes = 44 chars (with padding)
        byte[] decoded = java.util.Base64.getDecoder().decode(key);
        assertThat(decoded).hasSize(32);
    }

    @Test
    void wrongKeyLengthThrows() {
        assertThatThrownBy(() -> new AesGcmEncryption(new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
