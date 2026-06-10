package com.nexusflow.infra.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.CurrencyRateCache;
import com.nexusflow.domain.channel.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis-backed cache for exchange rates and supported currencies.
 * Uses manual JSON node construction to avoid coupling domain {@code @Value} classes
 * to Jackson deserialization requirements.
 *
 * Gracefully falls back to adapter on any Redis failure so that a cache outage
 * does not block payment processing.
 */
@Slf4j
public class RedisCurrencyRateCache implements CurrencyRateCache {

    private static final String RATE_KEY_PREFIX = "nexusflow:rate:";
    private static final String CURRENCY_KEY_PREFIX = "nexusflow:currency:";

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final long rateTtlSeconds;
    private final long currencyTtlSeconds;

    public RedisCurrencyRateCache(JedisPool jedisPool, long rateTtlSeconds, long currencyTtlSeconds) {
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        this.rateTtlSeconds = rateTtlSeconds;
        this.currencyTtlSeconds = currencyTtlSeconds;
    }

    @Override
    public ExchangeRate getExchangeRate(ChannelAdapter adapter, String token, String network, String quoteCurrency) {
        String key = rateKey(adapter.channelId(), token, network, quoteCurrency);
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(key);
            if (cached != null) {
                return parseRate(cached);
            }
        } catch (Exception e) {
            log.warn("Redis rate cache read failed for {}: {}", adapter.channelId(), e.getMessage());
        }

        ExchangeRate rate = adapter.getExchangeRate(token, network, quoteCurrency);
        if (rate != null) {
            cacheString(key, serializeRate(rate), rateTtlSeconds);
        }
        return rate;
    }

    @Override
    public List<CurrencyConfig> getSupportedCurrencies(ChannelAdapter adapter) {
        String key = currencyKey(adapter.channelId());
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(key);
            if (cached != null) {
                return parseCurrencies(cached);
            }
        } catch (Exception e) {
            log.warn("Redis currency cache read failed for {}: {}", adapter.channelId(), e.getMessage());
        }

        List<CurrencyConfig> currencies = adapter.getSupportedCurrencies();
        if (currencies != null) {
            cacheString(key, serializeCurrencies(currencies), currencyTtlSeconds);
        }
        return currencies;
    }

    // ── Serialization helpers ──

    private String serializeRate(ExchangeRate rate) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("token", rate.getToken());
        node.put("network", rate.getNetwork());
        node.put("price", rate.getPrice().toPlainString());
        node.put("quoteCurrency", rate.getQuoteCurrency());
        node.put("timestamp", rate.getTimestamp().toEpochMilli());
        return node.toString();
    }

    private ExchangeRate parseRate(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return ExchangeRate.builder()
                .token(node.get("token").asText())
                .network(node.get("network").asText())
                .price(new BigDecimal(node.get("price").asText()))
                .quoteCurrency(node.get("quoteCurrency").asText())
                .timestamp(Instant.ofEpochMilli(node.get("timestamp").asLong()))
                .build();
    }

    private String serializeCurrencies(List<CurrencyConfig> currencies) {
        ArrayNode array = objectMapper.createArrayNode();
        for (CurrencyConfig c : currencies) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("token", c.getToken());
            node.put("network", c.getNetwork());
            node.put("contractAddress", c.getContractAddress() != null ? c.getContractAddress() : "");
            node.put("decimals", c.getDecimals());
            node.put("minDeposit", c.getMinDeposit().toPlainString());
            node.put("requiredConfirmations", c.getRequiredConfirmations());
            node.put("enabled", c.isEnabled());
            array.add(node);
        }
        return array.toString();
    }

    private List<CurrencyConfig> parseCurrencies(String json) throws Exception {
        JsonNode array = objectMapper.readTree(json);
        List<CurrencyConfig> list = new ArrayList<>();
        for (JsonNode node : array) {
            String contractAddress = node.get("contractAddress").asText();
            list.add(CurrencyConfig.builder()
                    .token(node.get("token").asText())
                    .network(node.get("network").asText())
                    .contractAddress(contractAddress.isEmpty() ? null : contractAddress)
                    .decimals(node.get("decimals").asInt())
                    .minDeposit(new BigDecimal(node.get("minDeposit").asText()))
                    .requiredConfirmations(node.get("requiredConfirmations").asInt())
                    .enabled(node.get("enabled").asBoolean())
                    .build());
        }
        return list;
    }

    private void cacheString(String key, String value, long ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key {}: {}", key, e.getMessage());
        }
    }

    private String rateKey(String channelId, String token, String network, String quote) {
        return RATE_KEY_PREFIX + channelId + ":" + token + ":" + network + ":" + quote;
    }

    private String currencyKey(String channelId) {
        return CURRENCY_KEY_PREFIX + channelId;
    }
}
