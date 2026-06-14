package com.nexusflow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.fiat.FiatGateway;
import com.nexusflow.infra.fiat.HttpFiatRampGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class FiatRampGatewayConfig {

    @Value("${nexusflow.fiat-ramp.http.gateway-id:CUSTOM_HTTP_RAMP}")
    private String gatewayId;

    @Value("${nexusflow.fiat-ramp.http.display-name:Custom HTTP Ramp}")
    private String displayName;

    @Value("${nexusflow.fiat-ramp.http.base-url:}")
    private String baseUrl;

    @Value("${nexusflow.fiat-ramp.http.api-key:}")
    private String apiKey;

    @Value("${nexusflow.fiat-ramp.http.quote-path:/quotes}")
    private String quotePath;

    @Value("${nexusflow.fiat-ramp.http.order-path:/orders}")
    private String orderPath;

    @Value("${nexusflow.fiat-ramp.http.query-path:/orders}")
    private String queryPath;

    @Bean
    @Conditional(HttpFiatRampGatewayCondition.class)
    public FiatGateway httpFiatRampGateway(ObjectMapper objectMapper) {
        return new HttpFiatRampGateway(gatewayId, displayName, baseUrl, apiKey,
                quotePath, orderPath, queryPath, objectMapper);
    }

    static class HttpFiatRampGatewayCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return hasText(context.getEnvironment().getProperty("nexusflow.fiat-ramp.http.base-url", ""));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
