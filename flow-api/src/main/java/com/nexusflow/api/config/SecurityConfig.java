package com.nexusflow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.api.security.ApiKeyAuthFilter;
import com.nexusflow.api.security.CallbackHmacFilter;
import com.nexusflow.api.security.RateLimitFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Security configuration: API Key authentication + callback HMAC verification.
 *
 * API Key: validates {@code X-API-Key} header on merchant-facing endpoints.
 * HMAC: validates {@code X-Signature} header on {@code /callback/**} endpoints.
 */
@Configuration
public class SecurityConfig {

    @Value("${nexusflow.api.key:}")
    private String apiKey;

    @Value("${nexusflow.callback.hmac-secret.STUB:}")
    private String stubHmacSecret;

    @Value("${nexusflow.callback.hmac-secret.BITMART:}")
    private String bitmartHmacSecret;

    @Value("${nexusflow.callback.hmac-secret.COINBASE_COMMERCE:}")
    private String coinbaseCommerceHmacSecret;

    @Value("${nexusflow.callback.hmac-secret.SELF_HOSTED_NODE:}")
    private String selfHostedNodeHmacSecret;

    @Value("${nexusflow.callback.hmac-secret.MOONPAY:}")
    private String moonPayHmacSecret;

    @Value("${nexusflow.callback.hmac-secret.RAMP:}")
    private String rampHmacSecret;

    @Value("${nexusflow.callback.hmac-secret.BANXA:}")
    private String banxaHmacSecret;

    @Value("${nexusflow.api.rate-limit.per-minute:120}")
    private int rateLimitPerMinute;

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * API Key filter for merchant-facing endpoints.
     */
    @Bean
    public FilterRegistrationBean<Filter> apiKeyFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiKeyAuthFilter(apiKey, objectMapper));
        registration.addUrlPatterns("/pay/*", "/crypto/*", "/refund/*", "/ops/*", "/fiat/*");
        registration.setOrder(1);
        registration.setName("apiKeyAuthFilter");
        return registration;
    }

    /**
     * HMAC-SHA256 filter for channel callback endpoints.
     */
    @Bean
    public FilterRegistrationBean<Filter> callbackHmacFilterRegistration() {
        Map<String, String> secrets = Map.of(
                "STUB", stubHmacSecret,
                "BITMART", bitmartHmacSecret,
                "COINBASE_COMMERCE", coinbaseCommerceHmacSecret,
                "SELF_HOSTED_NODE", selfHostedNodeHmacSecret,
                "MOONPAY", moonPayHmacSecret,
                "RAMP", rampHmacSecret,
                "BANXA", banxaHmacSecret
        );
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CallbackHmacFilter(secrets, objectMapper));
        registration.addUrlPatterns("/callback/*");
        registration.setOrder(2);
        registration.setName("callbackHmacFilter");
        return registration;
    }

    /**
     * Rate limit filter — applies to all endpoints except actuator and cashier.
     */
    @Bean
    public FilterRegistrationBean<Filter> rateLimitFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(rateLimitPerMinute, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(0); // runs first
        registration.setName("rateLimitFilter");
        return registration;
    }
}
