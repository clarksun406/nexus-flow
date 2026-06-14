package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.ScannedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BitcoinAdapterTest {

    private RestTemplate restTemplate;
    private BitcoinAdapter adapter;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        adapter = new BitcoinAdapter("http://btc-node", "user", "pass", restTemplate);
    }

    @Test
    void scansBlocksAndParsesOutputs() {
        when(restTemplate.postForObject(eq("http://btc-node"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(result(101))
                .thenReturn(result("hash-101"))
                .thenReturn(result(Map.of(
                        "time", 1_700_000_000L,
                        "tx", List.of(Map.of(
                                "txid", "tx-1",
                                "vout", List.of(Map.of(
                                        "value", "0.00010000",
                                        "scriptPubKey", Map.of("address", "1BitcoinAddress")
                                ))
                        ))
                )))
                .thenReturn(result(101));

        List<ScannedTransaction> txs = adapter.scanNewBlocks(100);

        assertEquals(1, txs.size());
        ScannedTransaction tx = txs.get(0);
        assertEquals("tx-1", tx.getTxHash());
        assertEquals("1BitcoinAddress", tx.getToAddress());
        assertEquals("10000", tx.getAmount());
        assertEquals(101L, tx.getBlockNumber());
        assertEquals(1, tx.getConfirmations());
        assertEquals(1_700_000_000_000L, tx.getTimestamp());
    }

    @Test
    void confirmationsReadRawTransaction() {
        when(restTemplate.postForObject(eq("http://btc-node"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(result(Map.of("confirmations", 6)));

        assertEquals(6, adapter.getConfirmations("tx-1"));
    }

    @Test
    void unhealthyWhenRpcFails() {
        when(restTemplate.postForObject(eq("http://btc-node"), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("down"));

        assertFalse(adapter.isHealthy());
    }

    private Map<String, Object> result(Object value) {
        return Map.of("result", value);
    }
}
