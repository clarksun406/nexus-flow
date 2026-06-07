package com.nexusflow.wallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Base58Test {

    @Test
    void encodeDecodeRoundTrip() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, (byte) 0xFF, (byte) 0x80};
        assertArrayEquals(data, Base58.decode(Base58.encode(data)));
    }

    @Test
    void leadingZerosMapToOnes() {
        byte[] data = {0, 0, 1, 2, 3};
        String encoded = Base58.encode(data);
        assertTrue(encoded.startsWith("11"));
        assertArrayEquals(data, Base58.decode(encoded));
    }

    @Test
    void emptyInputEncodesToEmpty() {
        assertEquals("", Base58.encode(new byte[0]));
        assertArrayEquals(new byte[0], Base58.decode(""));
    }

    @Test
    void checkedRoundTrip() {
        byte[] payload = {0x41, 10, 20, 30, 40, 50, 60, 70, 80, 90};
        assertArrayEquals(payload, Base58.decodeChecked(Base58.encodeChecked(payload)));
    }

    @Test
    void corruptedPayloadFailsChecksum() {
        byte[] checked = Base58.decode(Base58.encodeChecked(new byte[]{0x41, 1, 2, 3}));
        checked[1] ^= 0xFF; // corrupt the payload, leaving the original checksum in place
        String corrupted = Base58.encode(checked);
        assertThrows(IllegalArgumentException.class, () -> Base58.decodeChecked(corrupted));
    }
}
