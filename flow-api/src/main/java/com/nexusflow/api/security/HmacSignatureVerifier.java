package com.nexusflow.api.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC-SHA256 signature verifier for channel callbacks.
 *
 * Channels sign their callback payloads with a shared secret;
 * this verifier checks the {@code X-Signature} header against the request body.
 */
public final class HmacSignatureVerifier {

    private static final String HMAC_ALGO = "HmacSHA256";

    private HmacSignatureVerifier() {}

    /**
     * Compute HMAC-SHA256 of {@code body} using {@code secret}, returned as lowercase hex.
     */
    public static String sign(String body, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    /**
     * Verify that {@code signature} matches the HMAC-SHA256 of {@code body} with {@code secret}.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @return true if signature is valid
     */
    public static boolean verify(String body, String secret, String signature) {
        if (signature == null || signature.isBlank()) return false;
        String expected = sign(body, secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
