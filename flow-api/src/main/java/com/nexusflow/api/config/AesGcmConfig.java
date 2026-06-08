package com.nexusflow.api.config;

import com.nexusflow.common.AesGcmEncryption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Encryption configuration.
 *
 * The encryption key MUST be provided via {@code nexusflow.encryption.key} (env var or config).
 * If omitted, the application refuses to start unless {@code nexusflow.encryption.allow-generated-key=true}
 * is explicitly set (intended for local development only).
 */
@Configuration
public class AesGcmConfig {

    @Value("${nexusflow.encryption.key:}")
    private String encryptionKey;

    @Value("${nexusflow.encryption.allow-generated-key:false}")
    private boolean allowGeneratedKey;

    @Bean
    public AesGcmEncryption aesGcmEncryption() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            if (!allowGeneratedKey) {
                throw new IllegalStateException(
                        "nexusflow.encryption.key is not set. "
                        + "Set it to a base64-encoded 32-byte key, or explicitly set "
                        + "nexusflow.encryption.allow-generated-key=true for development only.");
            }
            String generated = AesGcmEncryption.generateKey();
            System.err.println("WARNING: Using generated encryption key. "
                    + "Data encrypted with this key will be UNRECOVERABLE after restart. "
                    + "Set nexusflow.encryption.key for production.");
            return new AesGcmEncryption(generated);
        }
        return new AesGcmEncryption(encryptionKey);
    }
}
