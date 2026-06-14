package com.nexusflow.infra.fiat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusflow.domain.fiat.FiatGateway;
import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampOrderRequest;
import com.nexusflow.domain.fiat.FiatRampQuote;
import com.nexusflow.domain.fiat.FiatRampQuoteRequest;
import com.nexusflow.domain.fiat.FiatRampStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Normalized HTTP fiat-ramp gateway for provider proxies or providers with compatible JSON contracts.
 */
public class HttpFiatRampGateway implements FiatGateway {

    private final String gatewayId;
    private final String displayName;
    private final String baseUrl;
    private final String apiKey;
    private final String quotePath;
    private final String orderPath;
    private final String queryPath;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpFiatRampGateway(String gatewayId, String displayName, String baseUrl, String apiKey,
                               String quotePath, String orderPath, String queryPath,
                               ObjectMapper objectMapper) {
        this(gatewayId, displayName, baseUrl, apiKey, quotePath, orderPath, queryPath,
                new RestTemplate(), objectMapper);
    }

    HttpFiatRampGateway(String gatewayId, String displayName, String baseUrl, String apiKey,
                        String quotePath, String orderPath, String queryPath,
                        RestTemplate restTemplate, ObjectMapper objectMapper) {
        if (!hasText(gatewayId)) {
            throw new IllegalArgumentException("Fiat ramp gateway id is required");
        }
        if (!hasText(baseUrl)) {
            throw new IllegalArgumentException("Fiat ramp HTTP base URL is required");
        }
        this.gatewayId = normalizeCode(gatewayId);
        this.displayName = hasText(displayName) ? displayName.trim() : this.gatewayId;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.quotePath = normalizePath(quotePath, "/quotes");
        this.orderPath = normalizePath(orderPath, "/orders");
        this.queryPath = normalizePath(queryPath, "/orders");
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String gatewayId() {
        return gatewayId;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public FiatRampQuote quote(FiatRampQuoteRequest request) {
        JsonNode payload = payload(post(quotePath, quoteBody(request)));
        return FiatRampQuote.builder()
                .quoteId(firstText(payload, "quote_id", "quoteId", "id"))
                .providerId(firstTextOrDefault(payload, gatewayId, "provider_id", "providerId", "gateway_id", "gatewayId"))
                .direction(parseDirection(firstTextOrDefault(payload,
                        request.getDirection() != null ? request.getDirection().name() : null,
                        "direction")))
                .fiatAmount(decimalOrDefault(payload, request.getFiatAmount(), "fiat_amount", "fiatAmount"))
                .fiatCurrency(firstTextOrDefault(payload, request.getFiatCurrency(), "fiat_currency", "fiatCurrency"))
                .cryptoAmount(decimalOrDefault(payload, request.getCryptoAmount(), "crypto_amount", "cryptoAmount"))
                .token(firstTextOrDefault(payload, request.getToken(), "token", "crypto_currency", "cryptoCurrency"))
                .network(firstTextOrDefault(payload, request.getNetwork(), "network"))
                .exchangeRate(decimal(payload, "exchange_rate", "exchangeRate", "rate"))
                .feeAmountFiat(decimal(payload, "fee_amount_fiat", "feeAmountFiat", "fee"))
                .expiresAt(instant(payload, "expires_at", "expiresAt", "expire_time", "expireTime"))
                .build();
    }

    @Override
    public FiatRampOrder createOrder(FiatRampOrderRequest request) {
        JsonNode payload = payload(post(orderPath, orderBody(request)));
        String providerOrderId = firstText(payload, "provider_order_id", "providerOrderId", "order_id", "orderId", "id");
        if (!hasText(providerOrderId)) {
            throw new IllegalStateException("Fiat ramp HTTP response missing provider order id");
        }
        FiatRampOrder order = FiatRampOrder.builder()
                .rampOrderId(firstTextOrDefault(payload, gatewayId + "_" + UUID.randomUUID(),
                        "ramp_order_id", "rampOrderId"))
                .merchantId(firstTextOrDefault(payload, request.getMerchantId(), "merchant_id", "merchantId"))
                .merchantOrderNo(firstTextOrDefault(payload, request.getMerchantOrderNo(),
                        "merchant_order_no", "merchantOrderNo"))
                .paymentId(firstTextOrDefault(payload, request.getPaymentId(), "payment_id", "paymentId"))
                .direction(parseDirection(firstTextOrDefault(payload,
                        request.getDirection() != null ? request.getDirection().name() : null,
                        "direction")))
                .providerId(firstTextOrDefault(payload, gatewayId, "provider_id", "providerId", "gateway_id", "gatewayId"))
                .quoteId(firstTextOrDefault(payload, request.getQuoteId(), "quote_id", "quoteId"))
                .fiatAmount(decimalOrDefault(payload, request.getFiatAmount(), "fiat_amount", "fiatAmount"))
                .fiatCurrency(firstTextOrDefault(payload, request.getFiatCurrency(), "fiat_currency", "fiatCurrency"))
                .cryptoAmount(decimalOrDefault(payload, request.getCryptoAmount(), "crypto_amount", "cryptoAmount"))
                .token(firstTextOrDefault(payload, request.getToken(), "token", "crypto_currency", "cryptoCurrency"))
                .network(firstTextOrDefault(payload, request.getNetwork(), "network"))
                .exchangeRate(decimal(payload, "exchange_rate", "exchangeRate", "rate"))
                .feeAmountFiat(decimal(payload, "fee_amount_fiat", "feeAmountFiat", "fee"))
                .walletAddress(firstTextOrDefault(payload, request.getWalletAddress(), "wallet_address", "walletAddress"))
                .notifyUrl(firstTextOrDefault(payload, request.getNotifyUrl(), "notify_url", "notifyUrl"))
                .returnUrl(firstTextOrDefault(payload, request.getReturnUrl(), "return_url", "returnUrl"))
                .expireTime(instant(payload, "expires_at", "expiresAt", "expire_time", "expireTime"))
                .build();
        applyStatus(order, providerOrderId, firstText(payload, "checkout_url", "checkoutUrl", "payment_url", "paymentUrl"),
                parseStatus(firstText(payload, "status")), firstText(payload, "fiat_transfer_id", "fiatTransferId"),
                firstText(payload, "crypto_tx_hash", "cryptoTxHash"), firstText(payload, "failure_reason", "failureReason"));
        return order;
    }

    @Override
    public FiatRampOrder queryOrder(String providerOrderId) {
        JsonNode payload = payload(get(queryUrl(providerOrderId)));
        FiatRampStatus status = parseStatus(firstText(payload, "status"));
        return FiatRampOrder.reconstitute()
                .rampOrderId(firstTextOrDefault(payload, gatewayId + "_" + providerOrderId,
                        "ramp_order_id", "rampOrderId"))
                .merchantId(firstText(payload, "merchant_id", "merchantId"))
                .merchantOrderNo(firstText(payload, "merchant_order_no", "merchantOrderNo"))
                .paymentId(firstText(payload, "payment_id", "paymentId"))
                .direction(parseDirection(firstText(payload, "direction")))
                .providerId(firstTextOrDefault(payload, gatewayId, "provider_id", "providerId", "gateway_id", "gatewayId"))
                .providerOrderId(firstTextOrDefault(payload, providerOrderId,
                        "provider_order_id", "providerOrderId", "order_id", "orderId", "id"))
                .quoteId(firstText(payload, "quote_id", "quoteId"))
                .fiatAmount(decimal(payload, "fiat_amount", "fiatAmount"))
                .fiatCurrency(firstText(payload, "fiat_currency", "fiatCurrency"))
                .cryptoAmount(decimal(payload, "crypto_amount", "cryptoAmount"))
                .token(firstText(payload, "token", "crypto_currency", "cryptoCurrency"))
                .network(firstText(payload, "network"))
                .exchangeRate(decimal(payload, "exchange_rate", "exchangeRate", "rate"))
                .feeAmountFiat(decimal(payload, "fee_amount_fiat", "feeAmountFiat", "fee"))
                .walletAddress(firstText(payload, "wallet_address", "walletAddress"))
                .checkoutUrl(firstText(payload, "checkout_url", "checkoutUrl", "payment_url", "paymentUrl"))
                .fiatTransferId(firstText(payload, "fiat_transfer_id", "fiatTransferId"))
                .cryptoTxHash(firstText(payload, "crypto_tx_hash", "cryptoTxHash"))
                .notifyUrl(firstText(payload, "notify_url", "notifyUrl"))
                .returnUrl(firstText(payload, "return_url", "returnUrl"))
                .failureReason(firstText(payload, "failure_reason", "failureReason"))
                .status(status != null ? status : FiatRampStatus.PENDING_PAYMENT)
                .expireTime(instant(payload, "expires_at", "expiresAt", "expire_time", "expireTime"))
                .completeTime(instant(payload, "completed_at", "completedAt", "complete_time", "completeTime"))
                .createTime(instant(payload, "created_at", "createdAt"))
                .updateTime(instant(payload, "updated_at", "updatedAt"))
                .build();
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    private ObjectNode quoteBody(FiatRampQuoteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Fiat ramp quote request is required");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("merchant_id", request.getMerchantId());
        body.put("direction", request.getDirection() != null ? request.getDirection().name() : null);
        putDecimal(body, "fiat_amount", request.getFiatAmount());
        body.put("fiat_currency", request.getFiatCurrency());
        putDecimal(body, "crypto_amount", request.getCryptoAmount());
        body.put("token", request.getToken());
        body.put("network", request.getNetwork());
        body.put("wallet_address", request.getWalletAddress());
        body.put("country", request.getCountry());
        body.put("payment_method", request.getPaymentMethod());
        return body;
    }

    private ObjectNode orderBody(FiatRampOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Fiat ramp order request is required");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("merchant_id", request.getMerchantId());
        body.put("merchant_order_no", request.getMerchantOrderNo());
        body.put("payment_id", request.getPaymentId());
        body.put("direction", request.getDirection() != null ? request.getDirection().name() : null);
        body.put("quote_id", request.getQuoteId());
        putDecimal(body, "fiat_amount", request.getFiatAmount());
        body.put("fiat_currency", request.getFiatCurrency());
        putDecimal(body, "crypto_amount", request.getCryptoAmount());
        body.put("token", request.getToken());
        body.put("network", request.getNetwork());
        body.put("wallet_address", request.getWalletAddress());
        body.put("notify_url", request.getNotifyUrl());
        body.put("return_url", request.getReturnUrl());
        body.put("customer_reference", request.getCustomerReference());
        return body;
    }

    private JsonNode post(String path, JsonNode body) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + path,
                HttpMethod.POST,
                new HttpEntity<>(body, headers()),
                JsonNode.class);
        return responseBody(response, path);
    }

    private JsonNode get(String url) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                JsonNode.class);
        return responseBody(response, url);
    }

    private JsonNode responseBody(ResponseEntity<JsonNode> response, String target) {
        JsonNode body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty fiat ramp HTTP response for " + target);
        }
        return body;
    }

    private JsonNode payload(JsonNode response) {
        return response.has("data") ? response.path("data") : response;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Gateway-Id", gatewayId);
        if (hasText(apiKey)) {
            headers.set("X-API-Key", apiKey.trim());
        }
        return headers;
    }

    private String queryUrl(String providerOrderId) {
        if (!hasText(providerOrderId)) {
            throw new IllegalArgumentException("Fiat ramp provider order id is required");
        }
        return UriComponentsBuilder.fromHttpUrl(baseUrl + queryPath)
                .pathSegment(providerOrderId)
                .toUriString();
    }

    private void applyStatus(FiatRampOrder order, String providerOrderId, String checkoutUrl,
                             FiatRampStatus status, String fiatTransferId,
                             String cryptoTxHash, String failureReason) {
        if (hasText(providerOrderId) && order.getStatus() == FiatRampStatus.CREATED) {
            order.bindProviderOrder(providerOrderId, checkoutUrl);
        }
        FiatRampStatus target = status != null ? status : FiatRampStatus.PENDING_PAYMENT;
        switch (target) {
            case CREATED, PENDING_PAYMENT -> {
                // bindProviderOrder already moved a provider-backed order to PENDING_PAYMENT.
            }
            case PROCESSING -> order.markProcessing(fiatTransferId, cryptoTxHash);
            case COMPLETED -> order.markCompleted(fiatTransferId, cryptoTxHash);
            case FAILED -> order.markFailed(failureReason);
            case EXPIRED -> order.markExpired();
            case CANCELLED -> order.markCancelled();
        }
    }

    private void putDecimal(ObjectNode node, String field, BigDecimal value) {
        if (value != null) {
            node.put(field, value);
        } else {
            node.putNull(field);
        }
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        String value = firstText(node, fields);
        return hasText(value) ? new BigDecimal(value) : null;
    }

    private BigDecimal decimalOrDefault(JsonNode node, BigDecimal defaultValue, String... fields) {
        BigDecimal value = decimal(node, fields);
        return value != null ? value : defaultValue;
    }

    private Instant instant(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                long epoch = value.asLong();
                return epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
            }
            String text = value.asText(null);
            if (!hasText(text)) {
                continue;
            }
            try {
                return Instant.parse(text);
            } catch (RuntimeException ignored) {
                long epoch = Long.parseLong(text);
                return epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
            }
        }
        return null;
    }

    private FiatRampDirection parseDirection(String value) {
        return hasText(value) ? FiatRampDirection.valueOf(normalizeCode(value)) : null;
    }

    private FiatRampStatus parseStatus(String value) {
        return hasText(value) ? FiatRampStatus.valueOf(normalizeCode(value)) : null;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstTextOrDefault(JsonNode node, String defaultValue, String... fields) {
        String value = firstText(node, fields);
        return hasText(value) ? value : defaultValue;
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePath(String value, String defaultValue) {
        String path = hasText(value) ? value.trim() : defaultValue;
        return path.startsWith("/") ? path : "/" + path;
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
