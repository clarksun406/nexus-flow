package com.nexusflow.wallet;

import com.nexusflow.domain.shared.Chain;
import org.junit.jupiter.api.Test;
import org.web3j.utils.Numeric;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyGeneratorTest {

    private final KeyGenerator keyGenerator = new KeyGenerator();

    // Well-known secp256k1 test vector: private key = 1 → Ethereum address below.
    private static final String PK_ONE =
            "0000000000000000000000000000000000000000000000000000000000000001";
    private static final String ETH_ADDR_LOWER = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf";

    @Test
    void ethDerivationMatchesKnownVector() {
        String address = keyGenerator.deriveAddress(PK_ONE, Chain.ETH);
        assertTrue(address.startsWith("0x"));
        assertEquals(ETH_ADDR_LOWER, address.toLowerCase()); // case-insensitive vs EIP-55 checksum
    }

    @Test
    void tronDerivationSharesEthHashAndIsValidBase58Check() {
        String tron = keyGenerator.deriveAddress(PK_ONE, Chain.TRON);

        assertTrue(tron.startsWith("T"), "TRON mainnet addresses start with T");

        // decodeChecked throws if the checksum is invalid; payload = 0x41 ‖ 20-byte hash
        byte[] payload = Base58.decodeChecked(tron);
        assertEquals(21, payload.length);
        assertEquals(0x41, payload[0] & 0xFF);

        byte[] ethHash = Numeric.hexStringToByteArray(ETH_ADDR_LOWER); // 20 bytes
        byte[] tronHash = Arrays.copyOfRange(payload, 1, 21);
        assertArrayEquals(ethHash, tronHash, "TRON and ETH derive the same keccak hash");
    }

    @Test
    void generatePrivateKeyIs32BytesHex() {
        String pk = keyGenerator.generatePrivateKey(Chain.ETH);
        assertEquals(64, pk.length());
        assertTrue(pk.matches("[0-9a-f]{64}"));
    }

    @Test
    void generatedKeyProducesUsableEthAddress() {
        String pk = keyGenerator.generatePrivateKey(Chain.ETH);
        String address = keyGenerator.deriveAddress(pk, Chain.ETH);
        assertEquals(42, address.length()); // 0x + 40 hex chars
    }

    @Test
    void unsupportedChainThrows() {
        assertThrows(UnsupportedOperationException.class,
                () -> keyGenerator.deriveAddress(PK_ONE, Chain.BTC));
    }
}
