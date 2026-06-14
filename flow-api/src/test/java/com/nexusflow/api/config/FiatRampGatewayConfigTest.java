package com.nexusflow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.fiat.FiatGateway;
import com.nexusflow.infra.fiat.HttpFiatRampGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FiatRampGatewayConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(FiatRampGatewayConfig.class);

    @Test
    void httpFiatRampGatewayIsNotRegisteredWithoutBaseUrl() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(FiatGateway.class));
    }

    @Test
    void httpFiatRampGatewayIsRegisteredWhenBaseUrlIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "nexusflow.fiat-ramp.http.gateway-id=MOONPAY",
                        "nexusflow.fiat-ramp.http.display-name=MoonPay HTTP",
                        "nexusflow.fiat-ramp.http.base-url=https://ramp.test",
                        "nexusflow.fiat-ramp.http.api-key=api-key-1")
                .run(context -> {
                    assertThat(context).hasSingleBean(FiatGateway.class);
                    assertThat(context.getBean(FiatGateway.class)).isInstanceOf(HttpFiatRampGateway.class);
                });
    }
}
