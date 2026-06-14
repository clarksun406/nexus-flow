package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.ScannedTransaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Opt-in live-node smoke tests. They are skipped by default and only run when the
 * corresponding LIVE_* environment variable is configured.
 */
class LiveBlockchainAdapterTest {

    private static final String DEFAULT_ETH_USDT_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7";
    private static final String DEFAULT_TRON_USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";

    @Test
    void ethereumLiveRpcSmoke() {
        String rpcUrl = requireEnv("LIVE_ETH_RPC_URL");
        EthereumAdapter adapter = new EthereumAdapter(
                rpcUrl, envOrDefault("LIVE_ETH_USDT_CONTRACT", DEFAULT_ETH_USDT_CONTRACT));

        long height = adapter.getCurrentBlockHeight();
        assertTrue(height > 0, "ETH block height should be positive");
        assertTrue(adapter.isHealthy(), "ETH adapter should report healthy");

        String blockHash = adapter.getBlockHash(Math.max(0, height - 1));
        assertNotNull(blockHash, "ETH block hash should be available");
        assertFalse(blockHash.isBlank(), "ETH block hash should not be blank");

        List<ScannedTransaction> scanned = adapter.scanNewBlocks(Math.max(0, height - 1));
        assertNotNull(scanned, "ETH one-block scan should return a list");

        String txHash = System.getenv("LIVE_ETH_TX_HASH");
        if (hasText(txHash)) {
            assertTrue(adapter.getConfirmations(txHash) >= 0, "ETH confirmations should be non-negative");
        }
    }

    @Test
    void bitcoinLiveRpcSmoke() {
        String rpcUrl = requireEnv("LIVE_BTC_RPC_URL");
        BitcoinAdapter adapter = new BitcoinAdapter(
                rpcUrl, System.getenv("LIVE_BTC_RPC_USERNAME"), System.getenv("LIVE_BTC_RPC_PASSWORD"));

        long height = adapter.getCurrentBlockHeight();
        assertTrue(height > 0, "BTC block height should be positive");
        assertTrue(adapter.isHealthy(), "BTC adapter should report healthy");

        String blockHash = adapter.getBlockHash(Math.max(0, height - 1));
        assertNotNull(blockHash, "BTC block hash should be available");
        assertFalse(blockHash.isBlank(), "BTC block hash should not be blank");

        List<ScannedTransaction> scanned = adapter.scanNewBlocks(Math.max(0, height - 1));
        assertNotNull(scanned, "BTC one-block scan should return a list");

        String txHash = System.getenv("LIVE_BTC_TX_HASH");
        if (hasText(txHash)) {
            assertTrue(adapter.getConfirmations(txHash) >= 0, "BTC confirmations should be non-negative");
        }
    }

    @Test
    void tronLiveNodeSmoke() {
        String nodeUrl = requireEnv("LIVE_TRON_NODE_URL");
        TronAdapter adapter = new TronAdapter(
                new HttpTronGridClient(nodeUrl),
                envOrDefault("LIVE_TRON_USDT_CONTRACT", DEFAULT_TRON_USDT_CONTRACT));

        long height = adapter.getCurrentBlockHeight();
        assertTrue(height > 0, "TRON block height should be positive");
        assertTrue(adapter.isHealthy(), "TRON adapter should report healthy");

        List<ScannedTransaction> scanned = adapter.scanNewBlocks(Math.max(0, height - 1));
        assertNotNull(scanned, "TRON one-block scan should return a list");

        String txHash = System.getenv("LIVE_TRON_TX_HASH");
        if (hasText(txHash)) {
            assertTrue(adapter.getConfirmations(txHash) >= 0, "TRON confirmations should be non-negative");
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        assumeTrue(hasText(value), "Set " + name + " to run this live smoke test");
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return hasText(value) ? value : defaultValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
