package com.nexusflow.infra.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tron blockchain adapter (TRC20), backed by the TronGrid HTTP API via {@link TronGridClient}.
 *
 * Block-height, confirmation, health, and TRC20 Transfer event scanning are implemented and
 * unit-tested with the HTTP transport stubbed.
 */
@Slf4j
public class TronAdapter implements BlockchainAdapter {

    private final TronGridClient client;
    private final String usdtContractAddress;

    public TronAdapter(TronGridClient client, String usdtContractAddress) {
        this.client = client;
        this.usdtContractAddress = usdtContractAddress;
    }

    @Override
    public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
        long currentBlock = getCurrentBlockHeight();
        if (currentBlock <= lastScannedBlock) {
            return List.of();
        }

        List<ScannedTransaction> transactions = new ArrayList<>();
        for (long blockNumber = lastScannedBlock + 1; blockNumber <= currentBlock; blockNumber++) {
            transactions.addAll(scanTransferEventsAtBlock(blockNumber, currentBlock));
        }
        return transactions;
    }

    @Override
    public long getCurrentBlockHeight() {
        try {
            JsonNode node = client.post("/wallet/getnowblock", Map.of());
            return node.path("block_header").path("raw_data").path("number").asLong(0);
        } catch (Exception e) {
            log.error("Failed to get TRON block height: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int getConfirmations(String txHash) {
        try {
            JsonNode info = client.post("/wallet/gettransactioninfobyid", Map.of("value", txHash));
            long txBlock = info.path("blockNumber").asLong(0);
            if (txBlock <= 0) {
                return 0; // tx not yet packed into a block (or unknown)
            }
            long current = getCurrentBlockHeight();
            return current > txBlock ? (int) (current - txBlock) : 0;
        } catch (Exception e) {
            log.error("Failed to get TRON confirmations for {}: {}", txHash, e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean isHealthy() {
        return getCurrentBlockHeight() > 0;
    }

    @Override
    public Chain supportedChain() {
        return Chain.TRON;
    }

    private List<ScannedTransaction> scanTransferEventsAtBlock(long blockNumber, long currentBlock) {
        List<ScannedTransaction> transactions = new ArrayList<>();
        String fingerprint = null;
        do {
            Map<String, Object> query = new LinkedHashMap<>();
            query.put("event_name", "Transfer");
            query.put("only_confirmed", true);
            query.put("block_number", blockNumber);
            query.put("limit", 200);
            if (hasText(fingerprint)) {
                query.put("fingerprint", fingerprint);
            }

            JsonNode response = client.get("/v1/contracts/" + usdtContractAddress + "/events", query);
            JsonNode data = response.path("data");
            if (data.isArray()) {
                for (JsonNode event : data) {
                    parseTransferEvent(event, currentBlock).ifPresent(transactions::add);
                }
            }

            String next = response.path("meta").path("fingerprint").asText(null);
            fingerprint = hasText(next) && !next.equals(fingerprint) ? next : null;
        } while (hasText(fingerprint));

        return transactions;
    }

    private java.util.Optional<ScannedTransaction> parseTransferEvent(JsonNode event, long currentBlock) {
        JsonNode result = event.path("result");
        String txHash = event.path("transaction_id").asText(null);
        String from = result.path("from").asText(null);
        String to = result.path("to").asText(null);
        String value = result.path("value").asText(null);
        if (!hasText(txHash) || !hasText(to) || !hasText(value)) {
            log.debug("Skipping malformed TRON Transfer event: {}", event);
            return java.util.Optional.empty();
        }

        long blockNumber = event.path("block_number").asLong(0);
        if (blockNumber <= 0) {
            log.debug("Skipping TRON Transfer event without block number: {}", event);
            return java.util.Optional.empty();
        }
        int confirmations = currentBlock > blockNumber ? (int) (currentBlock - blockNumber) : 0;
        return java.util.Optional.of(ScannedTransaction.builder()
                .txHash(txHash)
                .fromAddress(from)
                .toAddress(to)
                .amount(value)
                .contractAddress(usdtContractAddress)
                .blockNumber(blockNumber)
                .confirmations(confirmations)
                .timestamp(event.path("block_timestamp").asLong(0))
                .build());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
