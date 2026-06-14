package com.nexusflow.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BlockchainConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BlockchainConfig.class);

    @Test
    void externalStubChannelBeansAreNotLoadedInProdProfile() throws Exception {
        assertHasNonProdProfile("bitMartAdapter");
        assertHasNonProdProfile("binancePayAdapter");
    }

    @Test
    void coinbaseNoKeyStubIsAllowedOutsideProdButNotInProd() {
        contextRunner.run(context ->
                assertThat(context).hasBean("coinbaseCommerceAdapter"));

        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean("coinbaseCommerceAdapter"));

        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "nexusflow.coinbase-commerce.api-key=api-key")
                .run(context -> assertThat(context).hasBean("coinbaseCommerceAdapter"));
    }

    private static void assertHasNonProdProfile(String methodName) throws Exception {
        Method method = BlockchainConfig.class.getDeclaredMethod(methodName);
        Profile profile = method.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(Arrays.asList(profile.value())).contains("!prod");
    }
}
