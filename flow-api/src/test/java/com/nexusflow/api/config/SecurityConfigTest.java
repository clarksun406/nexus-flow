package com.nexusflow.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(SecurityConfig.class);

    @Test
    void apiKeyFilterProtectsFiatRampEndpoints() {
        contextRunner.run(context -> {
            @SuppressWarnings("unchecked")
            FilterRegistrationBean<Filter> registration =
                    context.getBean("apiKeyFilterRegistration", FilterRegistrationBean.class);

            assertThat(registration.getUrlPatterns()).contains("/fiat/*");
        });
    }
}
