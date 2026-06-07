package com.nexusflow.wallet;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Base58 / Base58Check encoding (Bitcoin alphabet).
 *
 * Self-contained (JDK SHA-256 only) so wallet address derivation needs no Base58 library.
 * Used for TRON (and, later, BTC) address encoding.
 */
final class Base58 {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);

    private Base58() {}

    /** Plain Base58 encode. */
    static String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }
        // Count leading zero bytes — each maps to a leading '1'.
        int leadingZeros = 0;
        while (leadingZeros < input.length && input[leadingZeros] == 0) {
            leadingZeros++;
        }

        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (value.signum() > 0) {
            BigInteger[] divMod = value.divideAndRemainder(BASE);
            sb.append(ALPHABET.charAt(divMod[1].intValue()));
            value = divMod[0];
        }
        for (int i = 0; i < leadingZeros; i++) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }

    /** Plain Base58 decode. */
    static byte[] decode(String input) {
        if (input.isEmpty()) {
            return new byte[0];
        }
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < input.length(); i++) {
            int digit = ALPHABET.indexOf(input.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base58 character: " + input.charAt(i));
            }
            value = value.multiply(BASE).add(BigInteger.valueOf(digit));
        }
        byte[] bytes = value.toByteArray();
        // Strip a possible sign byte BigInteger may prepend.
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        int leadingOnes = 0;
        while (leadingOnes < input.length() && input.charAt(leadingOnes) == ALPHABET.charAt(0)) {
            leadingOnes++;
        }
        byte[] result = new byte[leadingOnes + bytes.length];
        System.arraycopy(bytes, 0, result, leadingOnes, bytes.length);
        return result;
    }

    /** Base58Check encode: appends a 4-byte double-SHA256 checksum before encoding. */
    static String encodeChecked(byte[] payload) {
        byte[] checksum = doubleSha256(payload);
        byte[] checked = new byte[payload.length + 4];
        System.arraycopy(payload, 0, checked, 0, payload.length);
        System.arraycopy(checksum, 0, checked, payload.length, 4);
        return encode(checked);
    }

    /** Base58Check decode: verifies and strips the 4-byte checksum, returning the payload. */
    static byte[] decodeChecked(String input) {
        byte[] checked = decode(input);
        if (checked.length < 4) {
            throw new IllegalArgumentException("Base58Check input too short");
        }
        byte[] payload = Arrays.copyOfRange(checked, 0, checked.length - 4);
        byte[] checksum = Arrays.copyOfRange(checked, checked.length - 4, checked.length);
        byte[] expected = Arrays.copyOfRange(doubleSha256(payload), 0, 4);
        if (!Arrays.equals(checksum, expected)) {
            throw new IllegalArgumentException("Base58Check checksum mismatch");
        }
        return payload;
    }

    private static byte[] doubleSha256(byte[] input) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(sha.digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
