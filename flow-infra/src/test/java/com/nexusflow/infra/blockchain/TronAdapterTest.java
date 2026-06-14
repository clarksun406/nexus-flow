package com.nexusflow.infra.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TronGrid response parsing. The HTTP transport ({@link HttpTronGridClient}) is
 * stubbed; these do not exercise the network.
 */
class TronAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TronGridClient client;
    private TronAdapter adapter;

    @BeforeEach
    void setUp() {
        client = mock(TronGridClient.class);
        adapter = new TronAdapter(client, "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t");
    }

    private static JsonNode json(String body) {
        try {
            return MAPPER.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void parsesCurrentBlockHeight() {
        when(client.post(eq("/wallet/getnowblock"), any()))
                .thenReturn(json("{\"block_header\":{\"raw_data\":{\"number\":65432100}}}"));

        assertEquals(65432100L, adapter.getCurrentBlockHeight());
    }

    @Test
    void currentBlockHeightIsZeroOnMalformedResponse() {
        when(client.post(eq("/wallet/getnowblock"), any())).thenReturn(json("{}"));
        assertEquals(0L, adapter.getCurrentBlockHeight());
    }

    @Test
    void computesConfirmationsAsCurrentMinusTxBlock() {
        when(client.post(eq("/wallet/gettransactioninfobyid"), any()))
                .thenReturn(json("{\"blockNumber\":65432088}"));
        when(client.post(eq("/wallet/getnowblock"), any()))
                .thenReturn(json("{\"block_header\":{\"raw_data\":{\"number\":65432100}}}"));

        assertEquals(12, adapter.getConfirmations("abc123"));
    }

    @Test
    void confirmationsZeroWhenTxNotYetInBlock() {
        when(client.post(eq("/wallet/gettransactioninfobyid"), any())).thenReturn(json("{}"));

        assertEquals(0, adapter.getConfirmations("pending-tx"));
    }

    @Test
    void confirmationsZeroWhenTxBlockAheadOfTip() {
        // defensive: tx block reported higher than current tip
        when(client.post(eq("/wallet/gettransactioninfobyid"), any()))
                .thenReturn(json("{\"blockNumber\":200}"));
        when(client.post(eq("/wallet/getnowblock"), any()))
                .thenReturn(json("{\"block_header\":{\"raw_data\":{\"number\":100}}}"));

        assertEquals(0, adapter.getConfirmations("tx"));
    }

    @Test
    void healthyWhenBlockHeightPositive() {
        when(client.post(eq("/wallet/getnowblock"), any()))
                .thenReturn(json("{\"block_header\":{\"raw_data\":{\"number\":1}}}"));
        assertTrue(adapter.isHealthy());
    }

    @Test
    void unhealthyWhenClientThrows() {
        when(client.post(eq("/wallet/getnowblock"), any()))
                .thenThrow(new RuntimeException("node down"));
        assertFalse(adapter.isHealthy());
    }

    @Test
    void scanNewBlocksParsesConfirmedTransferEvents() {
        when(client.post(eq("/wallet/getnowblock"), any()))
                .thenReturn(json("{\"block_header\":{\"raw_data\":{\"number\":105}}}"));
        when(client.get(eq("/v1/contracts/TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t/events"), any()))
                .thenAnswer(invocation -> {
                    Map<?, ?> query = invocation.getArgument(1);
                    long blockNumber = ((Number) query.get("block_number")).longValue();
                    if (blockNumber == 104L) {
                        return json("""
                                {
                                  "data": [
                                    {
                                      "transaction_id": "tx-104",
                                      "block_number": 104,
                                      "block_timestamp": 1710000000000,
                                      "event_name": "Transfer",
                                      "result": {
                                        "from": "TFROM",
                                        "to": "TTO",
                                        "value": "1000000"
                                      }
                                    }
                                  ],
                                  "meta": {}
                                }
                                """);
                    }
                    return json("{\"data\":[],\"meta\":{}}");
                });

        List<com.nexusflow.domain.blockchain.ScannedTransaction> transactions = adapter.scanNewBlocks(103L);

        assertEquals(1, transactions.size());
        var tx = transactions.get(0);
        assertEquals("tx-104", tx.getTxHash());
        assertEquals("TFROM", tx.getFromAddress());
        assertEquals("TTO", tx.getToAddress());
        assertEquals("1000000", tx.getAmount());
        assertEquals("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t", tx.getContractAddress());
        assertEquals(104L, tx.getBlockNumber());
        assertEquals(1, tx.getConfirmations());
        assertEquals(1710000000000L, tx.getTimestamp());
    }
}
