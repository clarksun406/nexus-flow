package com.nexusflow.api.config;

import com.nexusflow.common.AesGcmEncryption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Encryption configuration.
 *
 * The encryption key should be provided via environment variable or external config.
 * For development, a default key is used (NOT for production).
 */
@Configuration
public class AesGcmConfig {

    @Value("${nexusflow.encryption.key:}")
    private String encryptionKey;

    @Bean
    public AesGcmEncryption aesGcmEncryption() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            // Generate a key for dev; in production this must be provided externally
            String generated = AesGcmEncryption.generateKey();
            System.err.println("WARNING: Using generated encryption key. Set nexusflow.encryption.key in production.");
            return new AesGcmEncryption(generated);
        }
        return new AesGcmEncryption(encryptionKey);
    }
}