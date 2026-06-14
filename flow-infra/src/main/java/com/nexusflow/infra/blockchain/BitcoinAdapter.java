package com.nexusflow.infra.blockchain;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.blockchain.ScannedTransaction;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bitcoin blockchain adapter using Bitcoin Core JSON-RPC.
 */
@Slf4j
public class BitcoinAdapter implements BlockchainAdapter {

    private static final BigDecimal SATOSHIS_PER_BTC = new BigDecimal("100000000");

    private final String nodeUrl;
    private final String username;
    private final String password;
    private final RestTemplate restTemplate;

    public BitcoinAdapter(String nodeUrl) {
        this(nodeUrl, null, null, new RestTemplate());
    }

    public BitcoinAdapter(String nodeUrl, String username, String password) {
        this(nodeUrl, username, password, new RestTemplate());
    }

    BitcoinAdapter(String nodeUrl, String username, String password, RestTemplate restTemplate) {
        this.nodeUrl = nodeUrl;
        this.username = username;
        this.password = password;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<ScannedTransaction> scanNewBlocks(long lastScannedBlock) {
        long current = getCurrentBlockHeight();
        long from = Math.max(0, lastScannedBlock + 1);
        if (current <= 0 || from > current) {
            return List.of();
        }

        List<ScannedTransaction> transactions = new ArrayList<>();
        for (long height = from; height <= current; height++) {
            try {
                String blockHash = getBlockHash(height);
                if (blockHash == null) {
                    continue;
                }
                Map<String, Object> block = rpc("getblock", List.of(blockHash, 2));
                transactions.addAll(parseBlock(block, height, current));
            } catch (Exception e) {
                log.error("Failed to scan BTC block {}: {}", height, e.getMessage(), e);
                break;
            }
        }
        return transactions;
    }

    @Override
    public long getCurrentBlockHeight() {
        try {
            Object count = rpc("getblockcount", List.of());
            return toLong(count);
        } catch (Exception e) {
            log.error("Failed to get BTC block height: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int getConfirmations(String txHash) {
        try {
            Map<String, Object> tx = rpc("getrawtransaction", List.of(txHash, true));
            return Math.toIntExact(toLong(tx.get("confirmations")));
        } catch (Exception e) {
            log.error("Failed to get BTC confirmations for {}: {}", txHash, e.getMessage());
            return 0;
        }
    }

    @Override
    public String getBlockHash(long blockNumber) {
        try {
            Object hash = rpc("getblockhash", List.of(blockNumber));
            return hash != null ? hash.toString() : null;
        } catch (Exception e) {
            log.error("Failed to get BTC block hash for {}: {}", blockNumber, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isHealthy() {
        return getCurrentBlockHeight() > 0;
    }

    @Override
    public Chain supportedChain() {
        return Chain.BTC;
    }

    @SuppressWarnings("unchecked")
    private <T> T rpc(String method, List<?> params) {
        HttpHeaders headers = new HttpHeaders();
        if (username != null && !username.isBlank()) {
            headers.setBasicAuth(username, password != null ? password : "");
        }
        Map<String, Object> request = Map.of(
                "jsonrpc", "1.0",
                "id", "nexusflow",
                "method", method,
                "params", params);
        Map<String, Object> response = restTemplate.postForObject(nodeUrl, new HttpEntity<>(request, headers), Map.class);
        if (response == null) {
            throw new IllegalStateException("Empty Bitcoin RPC response for " + method);
        }
        Object error = response.get("error");
        if (error != null) {
            throw new IllegalStateException("Bitcoin RPC error for " + method + ": " + error);
        }
        return (T) response.get("result");
    }

    @SuppressWarnings("unchecked")
    private List<ScannedTransaction> parseBlock(Map<String, Object> block, long height, long currentHeight) {
        if (block == null) {
            return List.of();
        }
        long timestamp = toLong(block.get("time")) * 1000L;
        List<Object> txs = (List<Object>) block.getOrDefault("tx", List.of());
        List<ScannedTransaction> transactions = new ArrayList<>();
        for (Object txObj : txs) {
            if (!(txObj instanceof Map<?, ?> tx)) {
                continue;
            }
            String txHash = string(tx.get("txid"));
            Object outputsObj = tx.get("vout");
            List<Object> outputs = outputsObj instanceof List<?> list ? (List<Object>) list : List.of();
            for (Object outputObj : outputs) {
                if (!(outputObj instanceof Map<?, ?> output)) {
                    continue;
                }
                Optional<String> address = outputAddress(output);
                if (address.isEmpty()) {
                    continue;
                }
                transactions.add(ScannedTransaction.builder()
                        .txHash(txHash)
                        .fromAddress(null)
                        .toAddress(address.get())
                        .amount(toSatoshis(output.get("value")))
                        .contractAddress(null)
                        .blockNumber(height)
                        .confirmations(currentHeight >= height ? Math.toIntExact(currentHeight - height + 1) : 0)
                        .timestamp(timestamp)
                        .build());
            }
        }
        return transactions;
    }

    @SuppressWarnings("unchecked")
    private Optional<String> outputAddress(Map<?, ?> output) {
        Object scriptObj = output.get("scriptPubKey");
        if (!(scriptObj instanceof Map<?, ?> script)) {
            return Optional.empty();
        }
        String address = string(script.get("address"));
        if (address != null && !address.isBlank()) {
            return Optional.of(address);
        }
        Object addressesObj = script.get("addresses");
        if (addressesObj instanceof List<?> addresses && !addresses.isEmpty()) {
            return Optional.ofNullable(string(addresses.get(0)));
        }
        return Optional.empty();
    }

    private String toSatoshis(Object btcValue) {
        if (btcValue == null) {
            return "0";
        }
        BigDecimal btc = new BigDecimal(btcValue.toString());
        return btc.multiply(SATOSHIS_PER_BTC).setScale(0, RoundingMode.DOWN).toPlainString();
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String string(Object value) {
        return value != null ? value.toString() : null;
    }
}
