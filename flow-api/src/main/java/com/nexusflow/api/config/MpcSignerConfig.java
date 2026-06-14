package com.nexusflow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.wallet.MpcSigner;
import com.nexusflow.infra.wallet.HttpMpcSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class MpcSignerConfig {

    @Value("${nexusflow.mpc.http.sign-url:}")
    private String signUrl;

    @Value("${nexusflow.mpc.http.api-key:}")
    private String apiKey;

    @Bean
    @Conditional(HttpMpcSignerCondition.class)
    public MpcSigner httpMpcSigner(ObjectMapper objectMapper) {
        return new HttpMpcSigner(signUrl, apiKey, objectMapper);
    }

    static class HttpMpcSignerCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return hasText(context.getEnvironment().getProperty("nexusflow.mpc.http.sign-url", ""));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
