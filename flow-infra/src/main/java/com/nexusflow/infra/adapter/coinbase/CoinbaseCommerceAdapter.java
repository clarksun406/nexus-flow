package com.nexusflow.infra.adapter.coinbase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRefund;
import com.nexusflow.domain.channel.ChannelUser;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.DepositAddress;
import com.nexusflow.domain.channel.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Coinbase Commerce channel adapter.
 *
 * Without an API key it keeps the local stub behavior used by development and tests.
 * When an API key is configured, deposits and exchange rates are backed by Coinbase
 * Commerce REST calls.
 */
@Slf4j
public class CoinbaseCommerceAdapter implements ChannelAdapter {

    private static final String CHANNEL_ID = "COINBASE_COMMERCE";
    private static final String DEFAULT_BASE_URL = "https://api.commerce.coinbase.com";
    private static final String DEFAULT_API_VERSION = "2018-03-22";

    private final String baseUrl;
    private final String apiKey;
    private final String apiVersion;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CoinbaseCommerceAdapter() {
        this(DEFAULT_BASE_URL, null, DEFAULT_API_VERSION, new RestTemplate(), new ObjectMapper());
    }

    public CoinbaseCommerceAdapter(String baseUrl, String apiKey, String apiVersion) {
        this(baseUrl, apiKey, apiVersion, new RestTemplate(), new ObjectMapper());
    }

    CoinbaseCommerceAdapter(String baseUrl, String apiKey, String apiVersion,
                            RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.apiVersion = hasText(apiVersion) ? apiVersion.trim() : DEFAULT_API_VERSION;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String channelId() {
        return CHANNEL_ID;
    }

    @Override
    public String displayName() {
        return "Coinbase Commerce";
    }

    @Override
    public ChannelUser openUser(String merchantId, String buyerId) {
        return ChannelUser.builder()
                .channelUserId("CB_USER_" + buyerId)
                .channelId(channelId())
                .newlyCreated(!realMode())
                .build();
    }

    @Override
    public DepositAddress createDepositAddress(CreateDepositRequest req) {
        if (!realMode()) {
            return stubDepositAddress(req);
        }

        String currency = coinbaseCurrency(req.getToken(), req.getNetwork());
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", "NexusFlow " + req.getOrderId());
        request.put("description", "NexusFlow payment " + req.getOrderId());
        request.put("pricing_type", "fixed_price");
        ObjectNode localPrice = request.putObject("local_price");
        localPrice.put("amount", req.getCryptoAmount().toPlainString());
        localPrice.put("currency", currency);
        ObjectNode metadata = request.putObject("metadata");
        metadata.put("merchant_id", nullToEmpty(req.getMerchantId()));
        metadata.put("buyer_id", nullToEmpty(req.getBuyerId()));
        metadata.put("order_id", req.getOrderId());
        metadata.put("notify_url", nullToEmpty(req.getNotifyUrl()));

        JsonNode data = post("/charges", request).path("data");
        String address = selectAddress(data.path("addresses"), req.getToken(), req.getNetwork());
        String channelOrderId = firstText(data, "id", "code");
        if (!hasText(channelOrderId)) {
            throw new IllegalStateException("Coinbase charge response did not include id/code");
        }

        return DepositAddress.builder()
                .address(address)
                .channelOrderId(channelOrderId)
                .requiredConfirmations(requiredConfirmations(req.getToken(), req.getNetwork()))
                .build();
    }

    @Override
    public ChannelRefund refund(RefundRequest req) {
        if (!realMode()) {
            return ChannelRefund.builder()
                    .channelRefundId("CB_REFUND_" + req.getRefundOrderNo())
                    .status("PROCESSING")
                    .refundAmount(req.getRefundCryptoAmount())
                    .build();
        }

        log.warn("Coinbase Commerce refund requested for external processing: refundOrderNo={}, channelOrderId={}, token={}, network={}, amount={}, toAddress={}",
                req.getRefundOrderNo(), req.getChannelOrderId(), req.getToken(), req.getNetwork(),
                req.getRefundCryptoAmount(), req.getToAddress());
        return ChannelRefund.builder()
                .channelRefundId("CB_EXTERNAL_REFUND_" + req.getRefundOrderNo())
                .status("PROCESSING")
                .refundAmount(req.getRefundCryptoAmount())
                .build();
    }

    @Override
    public ChannelRefund queryRefund(String channelRefundId) {
        return ChannelRefund.builder()
                .channelRefundId(channelRefundId)
                .status(realMode() ? "PROCESSING" : "SUCCESS")
                .build();
    }

    @Override
    public List<CurrencyConfig> getSupportedCurrencies() {
        if (!realMode()) {
            return stubSupportedCurrencies();
        }
        return List.of(
                CurrencyConfig.builder()
                        .token("USDC").network("ERC20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("ETH").network("ERC20").decimals(18)
                        .minDeposit(new BigDecimal("0.001"))
                        .requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("BTC").network("BTC").decimals(8)
                        .minDeposit(new BigDecimal("0.0001"))
                        .requiredConfirmations(2).enabled(true).build()
        );
    }

    @Override
    public ExchangeRate getExchangeRate(String token, String network, String quoteCurrency) {
        if (!realMode()) {
            return ExchangeRate.builder()
                    .token(token)
                    .network(network)
                    .price(new BigDecimal("1.0000"))
                    .quoteCurrency(quoteCurrency)
                    .timestamp(Instant.now())
                    .build();
        }

        String currency = coinbaseCurrency(token, network);
        String quote = normalizeCode(quoteCurrency);
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/exchange-rates")
                .queryParam("currency", currency)
                .toUriString();
        JsonNode rates = get(url).path("data").path("rates");
        String price = rates.path(quote).asText(null);
        if (!hasText(price)) {
            throw new IllegalStateException("Coinbase exchange rate missing: " + currency + "/" + quote);
        }
        return ExchangeRate.builder()
                .token(normalizeCode(token))
                .network(normalizeCode(network))
                .price(new BigDecimal(price))
                .quoteCurrency(quote)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public boolean isHealthy() {
        if (!realMode()) {
            return true;
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/exchange-rates")
                    .queryParam("currency", "USD")
                    .toUriString();
            JsonNode response = get(url);
            return response != null && response.path("data").path("rates").isObject();
        } catch (RuntimeException e) {
            log.warn("Coinbase Commerce health check failed: {}", e.getMessage());
            return false;
        }
    }

    private DepositAddress stubDepositAddress(CreateDepositRequest req) {
        log.debug("COINBASE_COMMERCE createDepositAddress stub: orderId={}", req.getOrderId());
        return DepositAddress.builder()
                .address("0xCOINBASE_STUB_" + safePrefix(req.getOrderId(), 8))
                .channelOrderId("CB_CHARGE_" + req.getOrderId())
                .requiredConfirmations(12)
                .build();
    }

    private List<CurrencyConfig> stubSupportedCurrencies() {
        return List.of(
                CurrencyConfig.builder()
                        .token("USDC").network("ERC20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("USDT").network("ERC20").decimals(6)
                        .minDeposit(new BigDecimal("1"))
                        .requiredConfirmations(12).enabled(true).build(),
                CurrencyConfig.builder()
                        .token("BTC").network("BTC").decimals(8)
                        .minDeposit(new BigDecimal("0.0001"))
                        .requiredConfirmations(2).enabled(true).build()
        );
    }

    private JsonNode post(String path, JsonNode body) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + path,
                HttpMethod.POST,
                new HttpEntity<>(body, headers()),
                JsonNode.class);
        JsonNode responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("Empty Coinbase response for " + path);
        }
        return responseBody;
    }

    private JsonNode get(String url) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                JsonNode.class);
        JsonNode responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("Empty Coinbase response for " + url);
        }
        return responseBody;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-CC-Api-Key", apiKey.trim());
        headers.set("X-CC-Version", apiVersion);
        return headers;
    }

    private String coinbaseCurrency(String token, String network) {
        String normalizedToken = normalizeCode(token);
        String normalizedNetwork = normalizeCode(network);
        if ("BTC".equals(normalizedToken) && "BTC".equals(normalizedNetwork)) {
            return "BTC";
        }
        if ("ETH".equals(normalizedToken)
                && ("ETH".equals(normalizedNetwork) || "ERC20".equals(normalizedNetwork))) {
            return "ETH";
        }
        if ("USDC".equals(normalizedToken)
                && ("ETH".equals(normalizedNetwork) || "ERC20".equals(normalizedNetwork))) {
            return "USDC";
        }
        throw new IllegalArgumentException("Coinbase Commerce does not support " + token + " on " + network);
    }

    private String selectAddress(JsonNode addresses, String token, String network) {
        for (String key : addressKeys(token, network)) {
            String address = addresses.path(key).asText(null);
            if (hasText(address)) {
                return address;
            }
        }
        throw new IllegalStateException("Coinbase charge response did not include an address for "
                + token + "/" + network);
    }

    private List<String> addressKeys(String token, String network) {
        String currency = coinbaseCurrency(token, network);
        if ("BTC".equals(currency)) {
            return List.of("bitcoin", "btc");
        }
        if ("ETH".equals(currency)) {
            return List.of("ethereum", "eth");
        }
        if ("USDC".equals(currency)) {
            return List.of("usdc", "ethereum");
        }
        return List.of(currency.toLowerCase(Locale.ROOT));
    }

    private int requiredConfirmations(String token, String network) {
        String currency = coinbaseCurrency(token, network);
        return "BTC".equals(currency) ? 2 : 12;
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

    private boolean realMode() {
        return hasText(apiKey);
    }

    private String normalizeCode(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = hasText(value) ? value.trim() : DEFAULT_BASE_URL;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static String safePrefix(String value, int length) {
        if (value == null) {
            return "";
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
