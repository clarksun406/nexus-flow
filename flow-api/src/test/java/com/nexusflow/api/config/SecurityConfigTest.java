package com.nexusflow.api.config;

import com.nexusflow.api.security.ApiKeyHasher;
import com.nexusflow.api.security.CallbackHmacFilter;
import com.nexusflow.domain.merchant.MerchantCredentialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(MerchantCredentialRepository.class, () -> org.mockito.Mockito.mock(MerchantCredentialRepository.class))
            .withBean(ApiKeyHasher.class, ApiKeyHasher::new)
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

    @Test
    void callbackHmacFilterIncludesFiatRampProviders() {
        contextRunner.run(context -> {
            @SuppressWarnings("unchecked")
            FilterRegistrationBean<Filter> registration =
                    context.getBean("callbackHmacFilterRegistration", FilterRegistrationBean.class);
            CallbackHmacFilter filter = (CallbackHmacFilter) registration.getFilter();

            assertThat(channelSecrets(filter).keySet())
                    .contains("MOONPAY", "RAMP", "BANXA", "COINBASE_COMMERCE", "SELF_HOSTED_NODE");
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> channelSecrets(CallbackHmacFilter filter) throws Exception {
        Field field = CallbackHmacFilter.class.getDeclaredField("channelSecrets");
        field.setAccessible(true);
        return (Map<String, String>) field.get(filter);
    }
}
