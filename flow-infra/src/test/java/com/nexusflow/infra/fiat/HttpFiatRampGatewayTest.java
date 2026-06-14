package com.nexusflow.infra.fiat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrderRequest;
import com.nexusflow.domain.fiat.FiatRampQuoteRequest;
import com.nexusflow.domain.fiat.FiatRampStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpFiatRampGatewayTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestTemplate restTemplate;
    private HttpFiatRampGateway gateway;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        gateway = new HttpFiatRampGateway(
                "moonpay",
                "MoonPay HTTP",
                "https://ramp.test/",
                "api-key-1",
                "/quotes",
                "/orders",
                "/orders",
                restTemplate,
                MAPPER);
    }

    @Test
    void quotePostsNormalizedRequestAndParsesNestedResponse() throws Exception {
        when(restTemplate.exchange(eq("https://ramp.test/quotes"), eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<JsonNode>>any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(json("""
                        {
                          "data": {
                            "quote_id": "quote-1",
                            "provider_id": "MOONPAY",
                            "direction": "ON_RAMP",
                            "fiat_amount": "100.00",
                            "fiat_currency": "USD",
                            "crypto_amount": "98.50",
                            "token": "USDT",
                            "network": "TRC20",
                            "exchange_rate": "0.985",
                            "fee_amount_fiat": "1.50",
                            "expires_at": "2026-06-14T00:10:00Z"
                          }
                        }
                        """)));

        var quote = gateway.quote(FiatRampQuoteRequest.builder()
                .merchantId("merchant-1")
                .direction(FiatRampDirection.ON_RAMP)
                .fiatAmount(new BigDecimal("100.00"))
                .fiatCurrency("USD")
                .token("USDT")
                .network("TRC20")
                .walletAddress("TDEST")
                .country("US")
                .paymentMethod("CARD")
                .build());

        assertEquals("quote-1", quote.getQuoteId());
        assertEquals("MOONPAY", quote.getProviderId());
        assertEquals(FiatRampDirection.ON_RAMP, quote.getDirection());
        assertEquals(0, new BigDecimal("98.50").compareTo(quote.getCryptoAmount()));
        assertEquals(0, new BigDecimal("0.985").compareTo(quote.getExchangeRate()));
        assertEquals(Instant.parse("2026-06-14T00:10:00Z"), quote.getExpiresAt());

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<HttpEntity<JsonNode>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(restTemplate).exchange(eq("https://ramp.test/quotes"), eq(HttpMethod.POST),
                captor.capture(), eq(JsonNode.class));
        HttpEntity<JsonNode> entity = captor.getValue();
        assertEquals("api-key-1", entity.getHeaders().getFirst("X-API-Key"));
        assertEquals("MOONPAY", entity.getHeaders().getFirst("X-Gateway-Id"));
        assertEquals("merchant-1", entity.getBody().path("merchant_id").asText());
        assertEquals("ON_RAMP", entity.getBody().path("direction").asText());
        assertEquals(0, new BigDecimal("100.00").compareTo(entity.getBody().path("fiat_amount").decimalValue()));
        assertEquals("TDEST", entity.getBody().path("wallet_address").asText());
    }

    @Test
    void createOrderBindsProviderOrderAndMapsCheckoutUrl() throws Exception {
        when(restTemplate.exchange(eq("https://ramp.test/orders"), eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<JsonNode>>any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(json("""
                        {
                          "data": {
                            "ramp_order_id": "ramp-order-1",
                            "provider_order_id": "provider-order-1",
                            "checkout_url": "https://ramp.test/checkout/provider-order-1",
                            "status": "PENDING_PAYMENT",
                            "quote_id": "quote-1",
                            "crypto_amount": "98.50",
                            "exchange_rate": "0.985",
                            "fee_amount_fiat": "1.50",
                            "expires_at": 1781395800000
                          }
                        }
                        """)));

        var order = gateway.createOrder(FiatRampOrderRequest.builder()
                .merchantId("merchant-1")
                .merchantOrderNo("merchant-order-1")
                .paymentId("pay-1")
                .direction(FiatRampDirection.ON_RAMP)
                .quoteId("quote-1")
                .fiatAmount(new BigDecimal("100.00"))
                .fiatCurrency("USD")
                .token("USDT")
                .network("TRC20")
                .walletAddress("TDEST")
                .notifyUrl("https://merchant.example/ramp")
                .returnUrl("https://merchant.example/return")
                .customerReference("customer-1")
                .build());

        assertEquals("ramp-order-1", order.getRampOrderId());
        assertEquals("MOONPAY", order.getProviderId());
        assertEquals("provider-order-1", order.getProviderOrderId());
        assertEquals("https://ramp.test/checkout/provider-order-1", order.getCheckoutUrl());
        assertEquals(FiatRampStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(order.getFiatAmount()));
        assertEquals(0, new BigDecimal("98.50").compareTo(order.getCryptoAmount()));
        assertEquals("TDEST", order.getWalletAddress());

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<HttpEntity<JsonNode>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(restTemplate).exchange(eq("https://ramp.test/orders"), eq(HttpMethod.POST),
                captor.capture(), eq(JsonNode.class));
        JsonNode body = captor.getValue().getBody();
        assertEquals("merchant-order-1", body.path("merchant_order_no").asText());
        assertEquals("pay-1", body.path("payment_id").asText());
        assertEquals("customer-1", body.path("customer_reference").asText());
    }

    @Test
    void queryOrderParsesStatusAndSettlementReferences() throws Exception {
        when(restTemplate.exchange(eq("https://ramp.test/orders/provider-order-1"), eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<JsonNode>>any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(json("""
                        {
                          "data": {
                            "ramp_order_id": "ramp-order-1",
                            "provider_order_id": "provider-order-1",
                            "provider_id": "MOONPAY",
                            "direction": "ON_RAMP",
                            "status": "COMPLETED",
                            "fiat_amount": "100.00",
                            "fiat_currency": "USD",
                            "crypto_amount": "98.50",
                            "token": "USDT",
                            "network": "TRC20",
                            "fiat_transfer_id": "fiat-transfer-1",
                            "crypto_tx_hash": "0xtx",
                            "completed_at": "2026-06-14T00:11:00Z"
                          }
                        }
                        """)));

        var order = gateway.queryOrder("provider-order-1");

        assertEquals("ramp-order-1", order.getRampOrderId());
        assertEquals("provider-order-1", order.getProviderOrderId());
        assertEquals(FiatRampStatus.COMPLETED, order.getStatus());
        assertEquals("fiat-transfer-1", order.getFiatTransferId());
        assertEquals("0xtx", order.getCryptoTxHash());
        assertEquals(Instant.parse("2026-06-14T00:11:00Z"), order.getCompleteTime());
    }

    private JsonNode json(String body) throws Exception {
        return MAPPER.readTree(body);
    }
}
