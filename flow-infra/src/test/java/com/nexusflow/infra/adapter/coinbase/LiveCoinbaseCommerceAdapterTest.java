package com.nexusflow.infra.adapter.coinbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.channel.ChannelAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Opt-in Coinbase Commerce smoke checks. They are skipped by default and only
 * run when live Coinbase Commerce environment variables are configured.
 */
class LiveCoinbaseCommerceAdapterTest {

    private static final String DEFAULT_BASE_URL = "https://api.commerce.coinbase.com";
    private static final String DEFAULT_API_VERSION = "2018-03-22";

    @Test
    void coinbaseLiveRateSmoke() {
        CoinbaseCommerceAdapter adapter = liveAdapter();
        String token = envOrDefault("LIVE_COINBASE_COMMERCE_TOKEN", "BTC");
        String network = envOrDefault("LIVE_COINBASE_COMMERCE_NETWORK", "BTC");
        String quote = envOrDefault("LIVE_COINBASE_COMMERCE_QUOTE", "USD");

        var rate = adapter.getExchangeRate(token, network, quote);

        assertEquals(token.toUpperCase(), rate.getToken());
        assertEquals(network.toUpperCase(), rate.getNetwork());
        assertEquals(quote.toUpperCase(), rate.getQuoteCurrency());
        assertTrue(rate.getPrice().compareTo(BigDecimal.ZERO) > 0,
                "Coinbase Commerce live rate should be positive");
        assertTrue(adapter.isHealthy(), "Coinbase Commerce adapter should report healthy");
    }

    @Test
    void coinbaseLiveChargeSmoke() {
        assumeTrue(Boolean.parseBoolean(envOrDefault("LIVE_COINBASE_COMMERCE_CREATE_CHARGE", "false")),
                "Set LIVE_COINBASE_COMMERCE_CREATE_CHARGE=true to create a live Coinbase Commerce charge");

        CoinbaseCommerceAdapter adapter = liveAdapter();
        String token = envOrDefault("LIVE_COINBASE_COMMERCE_CHARGE_TOKEN", "USDC");
        String network = envOrDefault("LIVE_COINBASE_COMMERCE_CHARGE_NETWORK", "ERC20");
        String amount = envOrDefault("LIVE_COINBASE_COMMERCE_CHARGE_AMOUNT", "1");
        String orderId = "live-smoke-" + UUID.randomUUID();

        var deposit = adapter.createDepositAddress(ChannelAdapter.CreateDepositRequest.builder()
                .merchantId("live-smoke")
                .buyerId("live-smoke")
                .orderId(orderId)
                .token(token)
                .network(network)
                .cryptoAmount(new BigDecimal(amount))
                .notifyUrl("https://example.com/nexusflow-live-smoke")
                .build());

        assertFalse(deposit.getAddress().isBlank(), "Coinbase charge should include a deposit address");
        assertFalse(deposit.getChannelOrderId().isBlank(), "Coinbase charge should include an id/code");
        assertTrue(deposit.getRequiredConfirmations() > 0, "required confirmations should be positive");
    }

    private static CoinbaseCommerceAdapter liveAdapter() {
        return new CoinbaseCommerceAdapter(
                envOrDefault("LIVE_COINBASE_COMMERCE_BASE_URL",
                        envOrDefault("COINBASE_COMMERCE_BASE_URL", DEFAULT_BASE_URL)),
                requireApiKey(),
                envOrDefault("LIVE_COINBASE_COMMERCE_API_VERSION",
                        envOrDefault("COINBASE_COMMERCE_API_VERSION", DEFAULT_API_VERSION)),
                restTemplate(),
                new ObjectMapper());
    }

    private static RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);
        return new RestTemplate(requestFactory);
    }

    private static String requireApiKey() {
        String value = firstEnv("LIVE_COINBASE_COMMERCE_API_KEY", "COINBASE_COMMERCE_API_KEY");
        assumeTrue(hasText(value),
                "Set LIVE_COINBASE_COMMERCE_API_KEY or COINBASE_COMMERCE_API_KEY to run Coinbase live smoke tests");
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return hasText(value) ? value : defaultValue;
    }

    private static String firstEnv(String first, String second) {
        String value = System.getenv(first);
        if (hasText(value)) {
            return value;
        }
        return System.getenv(second);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
