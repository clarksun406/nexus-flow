package com.nexusflow.infra.adapter.coinbase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.channel.ChannelAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoinbaseCommerceAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CoinbaseCommerceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CoinbaseCommerceAdapter();
    }

    @Test
    void createDepositAddressReturnsChargeReference() {
        ChannelAdapter.CreateDepositRequest request = ChannelAdapter.CreateDepositRequest.builder()
                .orderId("order-1234567890")
                .token("USDC")
                .network("ERC20")
                .cryptoAmount(new BigDecimal("10"))
                .build();

        var deposit = adapter.createDepositAddress(request);

        assertEquals("0xCOINBASE_STUB_order-12", deposit.getAddress());
        assertEquals("CB_CHARGE_order-1234567890", deposit.getChannelOrderId());
        assertEquals(12, deposit.getRequiredConfirmations());
    }

    @Test
    void supportedCurrenciesIncludeUsdcErc20AndBtc() {
        var currencies = adapter.getSupportedCurrencies();

        assertTrue(currencies.stream().anyMatch(c ->
                "USDC".equals(c.getToken()) && "ERC20".equals(c.getNetwork())));
        assertTrue(currencies.stream().anyMatch(c ->
                "BTC".equals(c.getToken()) && "BTC".equals(c.getNetwork())));
    }

    @Test
    void getExchangeRateReturnsStubQuote() {
        var rate = adapter.getExchangeRate("USDC", "ERC20", "USD");

        assertEquals("USDC", rate.getToken());
        assertEquals("ERC20", rate.getNetwork());
        assertEquals("USD", rate.getQuoteCurrency());
        assertEquals(0, BigDecimal.ONE.compareTo(rate.getPrice()));
    }

    @Test
    void createDepositAddressCallsChargeApiInRealMode() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        CoinbaseCommerceAdapter realAdapter = new CoinbaseCommerceAdapter(
                "https://commerce.test/", "api-key", "2018-03-22", restTemplate, MAPPER);
        when(restTemplate.exchange(eq("https://commerce.test/charges"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(json("""
                        {
                          "data": {
                            "id": "charge-id-1",
                            "code": "CODE1",
                            "addresses": {
                              "usdc": "0xUSDCADDRESS",
                              "ethereum": "0xETHADDRESS"
                            }
                          }
                        }
                        """)));

        ChannelAdapter.CreateDepositRequest request = ChannelAdapter.CreateDepositRequest.builder()
                .merchantId("merchant-1")
                .buyerId("buyer-1")
                .orderId("order-coinbase-real")
                .token("USDC")
                .network("ERC20")
                .cryptoAmount(new BigDecimal("12.34"))
                .notifyUrl("https://merchant.example/callback")
                .build();

        var deposit = realAdapter.createDepositAddress(request);

        assertEquals("0xUSDCADDRESS", deposit.getAddress());
        assertEquals("charge-id-1", deposit.getChannelOrderId());
        assertEquals(12, deposit.getRequiredConfirmations());

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(eq("https://commerce.test/charges"), eq(HttpMethod.POST),
                entityCaptor.capture(), eq(JsonNode.class));
        assertEquals("api-key", entityCaptor.getValue().getHeaders().getFirst("X-CC-Api-Key"));
        JsonNode body = MAPPER.valueToTree(entityCaptor.getValue().getBody());
        assertEquals("fixed_price", body.get("pricing_type").asText());
        assertEquals("12.34", body.get("local_price").get("amount").asText());
        assertEquals("USDC", body.get("local_price").get("currency").asText());
        assertEquals("order-coinbase-real", body.get("metadata").get("order_id").asText());
    }

    @Test
    void getExchangeRateReadsCoinbaseRatesInRealMode() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        CoinbaseCommerceAdapter realAdapter = new CoinbaseCommerceAdapter(
                "https://commerce.test", "api-key", "2018-03-22", restTemplate, MAPPER);
        when(restTemplate.exchange(eq("https://commerce.test/exchange-rates?currency=BTC"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(json("""
                        {"data":{"currency":"BTC","rates":{"USD":"65000.25"}}}
                        """)));

        var rate = realAdapter.getExchangeRate("BTC", "BTC", "USD");

        assertEquals("BTC", rate.getToken());
        assertEquals("BTC", rate.getNetwork());
        assertEquals("USD", rate.getQuoteCurrency());
        assertEquals(0, new BigDecimal("65000.25").compareTo(rate.getPrice()));
    }

    @Test
    void realModeSupportedCurrenciesDoNotAdvertiseUsdt() {
        CoinbaseCommerceAdapter realAdapter = new CoinbaseCommerceAdapter(
                "https://commerce.test", "api-key", "2018-03-22", mock(RestTemplate.class), MAPPER);

        var currencies = realAdapter.getSupportedCurrencies();

        assertFalse(currencies.stream().anyMatch(c -> "USDT".equals(c.getToken())));
        assertTrue(currencies.stream().anyMatch(c -> "USDC".equals(c.getToken()) && "ERC20".equals(c.getNetwork())));
        assertTrue(currencies.stream().anyMatch(c -> "BTC".equals(c.getToken()) && "BTC".equals(c.getNetwork())));
    }

    private JsonNode json(String body) throws Exception {
        return MAPPER.readTree(body);
    }
}
